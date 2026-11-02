package tiameds.pharmabackend.service.impl.financialReport.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Writes a report as an .xlsx workbook. SXSSF keeps only a window of rows in
// memory and spills the rest to a temp file, so large reports stay cheap.
// Amounts, counts and dates are written as typed cells.
class ExcelReportWriter implements ReportTableWriter {

    private static final int ROWS_IN_MEMORY = 100;
    private static final int COLUMN_WIDTH = 20;

    private final OutputStream out;
    private final SXSSFWorkbook workbook;
    private final SXSSFSheet sheet;

    private final CellStyle titleStyle;
    private final CellStyle labelStyle;
    private final CellStyle headerStyle;
    private final CellStyle amountStyle;
    private final CellStyle countStyle;
    private final CellStyle dateTimeStyle;
    private final CellStyle dateStyle;

    private int rowIndex;


    ExcelReportWriter(OutputStream out) {

        this.out = out;
        this.workbook = new SXSSFWorkbook(ROWS_IN_MEMORY);
        this.sheet = workbook.createSheet("Report");
        sheet.setDefaultColumnWidth(COLUMN_WIDTH);

        Font bold = workbook.createFont();
        bold.setBold(true);

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        labelStyle = workbook.createCellStyle();
        labelStyle.setFont(bold);

        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bold);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        short amountFormat = workbook.createDataFormat().getFormat("#,##0.00");
        short countFormat = workbook.createDataFormat().getFormat("0");
        short dateTimeFormat = workbook.createDataFormat().getFormat("dd-mmm-yyyy hh:mm AM/PM");
        short dateFormat = workbook.createDataFormat().getFormat("dd-mmm-yyyy");

        amountStyle = workbook.createCellStyle();
        amountStyle.setDataFormat(amountFormat);

        countStyle = workbook.createCellStyle();
        countStyle.setDataFormat(countFormat);

        dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(dateTimeFormat);

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(dateFormat);
    }


    @Override
    public void title(String title) {

        Cell cell = sheet.createRow(rowIndex++).createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(titleStyle);
    }


    @Override
    public void line(String label, Object value) {

        Row row = sheet.createRow(rowIndex++);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        write(row.createCell(1), value);
    }


    @Override
    public void blank() {
        rowIndex++;
    }


    @Override
    public void header(List<String> headers) {

        Row row = sheet.createRow(rowIndex++);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }
    }


    @Override
    public void row(List<?> values) {

        Row row = sheet.createRow(rowIndex++);

        for (int i = 0; i < values.size(); i++) {
            write(row.createCell(i), values.get(i));
        }
    }


    @Override
    public void finish() throws IOException {
        workbook.write(out);
        out.flush();
    }


    // Also removes SXSSF's temp file.
    @Override
    public void close() throws IOException {
        workbook.close();
    }


    private void write(Cell cell, Object value) {

        if (value == null) {
            return;
        }

        if (value instanceof BigDecimal amount) {
            cell.setCellValue(amount.doubleValue());
            cell.setCellStyle(amountStyle);
        } else if (value instanceof Long || value instanceof Integer) {
            cell.setCellValue(((Number) value).doubleValue());
            cell.setCellStyle(countStyle);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
