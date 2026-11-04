package tiameds.pharmabackend.service.product.bulk;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the shapes the real exports arrive in: a BOM on the first header, quoted
 * fields containing the delimiter, spreadsheet-mangled numbers, and dd/MM/yyyy dates.
 */
class CsvFileTest {

    private CsvFile parse(String csv) throws IOException {
        return CsvFile.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void stripsBomAndReadsHeadersCaseInsensitively() throws IOException {
        CsvFile csv = parse("﻿Product Code,Product Name\nAPLUSP,A PLUS P\n");

        assertEquals("Product Code", csv.getHeaders().get(0));
        assertTrue(csv.hasHeader("product code"));
        assertEquals("A PLUS P", csv.getRows().get(0).get("PRODUCT NAME"));
    }

    @Test
    void keepsCommasInsideQuotedFields() throws IOException {
        CsvFile csv = parse("Product Name,Category\n"
                + "\"NEUROLAB AMP(b1,b6,b12,VITa)\",Drugs\n");

        CsvFile.Row row = csv.getRows().get(0);
        assertEquals("NEUROLAB AMP(b1,b6,b12,VITa)", row.get("Product Name"));
        assertEquals("Drugs", row.get("Category"));
    }

    @Test
    void unescapesDoubledQuotesAndSpansNewlinesInsideQuotes() throws IOException {
        CsvFile csv = parse("A,B\n\"say \"\"hi\"\"\",\"two\nlines\"\n");

        CsvFile.Row row = csv.getRows().get(0);
        assertEquals("say \"hi\"", row.get("A"));
        assertEquals("two\nlines", row.get("B"));
    }

    @Test
    void blankCellsReadAsNullAndBlankLinesAreDropped() throws IOException {
        CsvFile csv = parse("A,B,C\n1,,3\n\n,,\n");

        assertEquals(1, csv.getRows().size());
        assertNull(csv.getRows().get(0).get("B"));
        assertEquals("3", csv.getRows().get(0).get("C"));
    }

    @Test
    void numbersLineNumbersAsASpreadsheetDoes() throws IOException {
        CsvFile csv = parse("A\n1\n2\n");

        assertEquals(2, csv.getRows().get(0).getLineNumber());
        assertEquals(3, csv.getRows().get(1).getLineNumber());
    }

    @Test
    void readsTheFinalRowWithoutATrailingNewline() throws IOException {
        CsvFile csv = parse("A,B\n1,2");

        assertEquals(1, csv.getRows().size());
        assertEquals("2", csv.getRows().get(0).get("B"));
    }

    @Test
    void codeDropsTheSpreadsheetsTrailingPointZero() throws IOException {
        CsvFile.Row row = parse("HSN Code\n3004.0\n").getRows().get(0);

        assertEquals("3004", CellCoercion.code(row, "HSN Code"));
    }

    @Test
    void codeLeavesRealDecimalsAndTextAlone() throws IOException {
        CsvFile csv = parse("A,B\n3004.5,ABC123\n");
        CsvFile.Row row = csv.getRows().get(0);

        assertEquals("3004.5", CellCoercion.code(row, "A"));
        assertEquals("ABC123", CellCoercion.code(row, "B"));
    }

    @Test
    void integerAcceptsWholeFloatsAndRejectsFractions() throws IOException {
        CsvFile csv = parse("A,B\n10.0,10.5\n");
        CsvFile.Row row = csv.getRows().get(0);

        assertEquals(10L, CellCoercion.integer(row, "A"));
        assertThrows(CellCoercion.BulkValueException.class, () -> CellCoercion.integer(row, "B"));
    }

    @Test
    void readsIndianStyleDatesAndMonthOnlyExpiries() throws IOException {
        CsvFile csv = parse("A,B,C\n30/09/2027,2027-09-30,09/2027\n");
        CsvFile.Row row = csv.getRows().get(0);

        assertEquals(LocalDate.of(2027, 9, 30), CellCoercion.date(row, "A"));
        assertEquals(LocalDate.of(2027, 9, 30), CellCoercion.date(row, "B"));
        // A month-only expiry means the end of that month.
        assertEquals(LocalDate.of(2027, 9, 30), CellCoercion.date(row, "C"));
    }

    @Test
    void rejectsAnUnreadableDateAgainstItsColumn() throws IOException {
        CsvFile.Row row = parse("Expiry Date\nsometime\n").getRows().get(0);

        CellCoercion.BulkValueException e = assertThrows(CellCoercion.BulkValueException.class,
                () -> CellCoercion.date(row, "Expiry Date"));
        assertTrue(e.getMessage().contains("Expiry Date"));
        assertTrue(e.getMessage().contains("sometime"));
    }

    @Test
    void readsYesNoFlags() throws IOException {
        CsvFile csv = parse("A,B,C\nYes,no,maybe\n");
        CsvFile.Row row = csv.getRows().get(0);

        assertEquals(Boolean.TRUE, CellCoercion.flag(row, "A"));
        assertEquals(Boolean.FALSE, CellCoercion.flag(row, "B"));
        assertThrows(CellCoercion.BulkValueException.class, () -> CellCoercion.flag(row, "C"));
    }

    @Test
    void splitsMultiValueCellsOnPipeAndSemicolonButNotComma() throws IOException {
        CsvFile csv = parse("A,B\nFace|Neck; Scalp,\"Oily, Dry\"\n");
        CsvFile.Row row = csv.getRows().get(0);

        assertEquals(List.of("Face", "Neck", "Scalp"), CellCoercion.multi(row, "A"));
        // A comma is the delimiter, so a quoted cell containing one stays a single value.
        assertEquals(List.of("Oily, Dry"), CellCoercion.multi(row, "B"));
    }

    @Test
    void readsMoleculesWithAndWithoutStrength() throws IOException {
        CsvFile.Row row = parse("Molecules\nParacetamol:500mg|Caffeine\n").getRows().get(0);

        List<CellCoercion.MoleculeEntry> molecules = CellCoercion.molecules(row, "Molecules");
        assertEquals(2, molecules.size());
        assertEquals("Paracetamol", molecules.get(0).name());
        assertEquals("500mg", molecules.get(0).strength());
        assertEquals("Caffeine", molecules.get(1).name());
        assertNull(molecules.get(1).strength());
    }
}
