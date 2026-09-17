package tiameds.pharmabackend.service.product.bulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Case-insensitive name → id index over the master tables, built once per upload.
 * <p>
 * The bulk template is written by humans and so carries master data by name
 * ("Tablet", "Strip", "India"), while the product DTOs take master ids. Resolving
 * through one in-memory snapshot keeps a 2000-row file to a couple of dozen
 * queries instead of tens of thousands, and lets an unknown name degrade into a
 * row warning rather than an exception.
 * <p>
 * Masters that hang off a parent (a sub-type under its type, a device sub-category
 * under its category) are indexed twice: once under {@code parentId|name} and once
 * under the bare name. The scoped lookup is tried first so the same name under two
 * parents resolves correctly; the bare fallback keeps files working against master
 * data that was seeded without the parent link.
 */
public final class MasterIndex {

    private final Map<String, Map<String, Long>> scoped = new HashMap<>();
    private final Map<String, Map<String, Long>> flat = new HashMap<>();

    // Names of the indexes, so a typo is a compile error rather than a silent miss.
    static final String PRODUCT_CATEGORY = "productCategory";
    static final String PURCHASE_SMALLEST_UNIT = "purchaseSmallestUnit";
    static final String DOSAGE_FORM = "dosageForm";
    static final String MOLECULE = "molecule";
    static final String THERAPEUTIC_CATEGORY = "therapeuticCategory";
    static final String THERAPEUTIC_SUBCATEGORY = "therapeuticSubcategory";
    static final String FLAVOUR = "flavour";
    static final String AGE_GROUP = "ageGroup";
    static final String COUNTRY = "country";
    static final String SKIN_TYPE = "skinType";
    static final String HAIR_TYPE = "hairType";
    static final String INTENDED_USE_AREA = "intendedUseArea";
    static final String POWER_SOURCE = "powerSource";
    static final String NET_QUANTITY_UNIT = "netQuantityUnit";
    static final String MATERIAL_TYPE = "materialType";
    static final String PRODUCT_TYPE = "productType";
    static final String PRODUCT_SUB_TYPE = "productSubType";
    static final String PRODUCT_FORM = "productForm";
    static final String DEVICE_CATEGORY = "deviceCategory";
    static final String DEVICE_SUB_CATEGORY = "deviceSubCategory";
    static final String DEVICE_SPECIFICATION_UNIT = "deviceSpecificationUnit";

    /**
     * Indexes a master with no parent: name → id.
     */
    <T> void put(String index, List<T> rows, Function<T, String> name, Function<T, Long> id) {
        Map<String, Long> byName = flat.computeIfAbsent(index, k -> new HashMap<>());
        for (T row : rows) {
            String n = normalize(name.apply(row));
            if (n != null) {
                byName.putIfAbsent(n, id.apply(row));
            }
        }
    }

    /**
     * Indexes a master under its parent id as well as bare, for the fallback described above.
     */
    <T> void putScoped(String index, List<T> rows,
                       Function<T, Long> parentId, Function<T, String> name, Function<T, Long> id) {
        Map<String, Long> byScopedName = scoped.computeIfAbsent(index, k -> new HashMap<>());
        Map<String, Long> byName = flat.computeIfAbsent(index, k -> new HashMap<>());
        for (T row : rows) {
            String n = normalize(name.apply(row));
            if (n == null) {
                continue;
            }
            Long parent = parentId.apply(row);
            if (parent != null) {
                byScopedName.putIfAbsent(parent + "|" + n, id.apply(row));
            }
            byName.putIfAbsent(n, id.apply(row));
        }
    }

    /**
     * Indexes the purchase/smallest unit pair, which is keyed by two names at once
     * ("Strip" of "Tablet") within a product category.
     */
    <T> void putUnitPair(List<T> rows, Function<T, Long> categoryId,
                         Function<T, String> purchaseUnitName, Function<T, String> smallestUnitName,
                         Function<T, Long> id) {
        Map<String, Long> byScopedName = scoped.computeIfAbsent(PURCHASE_SMALLEST_UNIT, k -> new HashMap<>());
        Map<String, Long> byName = flat.computeIfAbsent(PURCHASE_SMALLEST_UNIT, k -> new HashMap<>());
        for (T row : rows) {
            String pair = unitPairKey(purchaseUnitName.apply(row), smallestUnitName.apply(row));
            if (pair == null) {
                continue;
            }
            Long parent = categoryId.apply(row);
            if (parent != null) {
                byScopedName.putIfAbsent(parent + "|" + pair, id.apply(row));
            }
            byName.putIfAbsent(pair, id.apply(row));
        }
    }

    static String unitPairKey(String purchaseUnit, String smallestUnit) {
        String p = normalize(purchaseUnit);
        String s = normalize(smallestUnit);
        if (p == null || s == null) {
            return null;
        }
        return p + "»" + s;
    }

    /**
     * Looks the name up without a parent scope.
     */
    public Long find(String index, String name) {
        String n = normalize(name);
        if (n == null) {
            return null;
        }
        Map<String, Long> byName = flat.get(index);
        return byName == null ? null : byName.get(n);
    }

    /**
     * Looks the name up under {@code parentId}, falling back to the unscoped index.
     */
    public Long findScoped(String index, Long parentId, String name) {
        String n = normalize(name);
        if (n == null) {
            return null;
        }
        if (parentId != null) {
            Map<String, Long> byScopedName = scoped.get(index);
            if (byScopedName != null) {
                Long hit = byScopedName.get(parentId + "|" + n);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return find(index, n);
    }

    /**
     * Resolves the {@code PurchaseSmallestUnit} for a purchase/smallest unit pair
     * within a product category.
     */
    public Long findUnitPair(Long productCategoryId, String purchaseUnit, String smallestUnit) {
        String pair = unitPairKey(purchaseUnit, smallestUnit);
        if (pair == null) {
            return null;
        }
        return findScoped(PURCHASE_SMALLEST_UNIT, productCategoryId, pair);
    }

    /**
     * Resolves each name in turn, collecting the ones that matched. Names that did
     * not resolve are appended to {@code unresolved} so the row can warn about them.
     */
    public List<Long> findAll(String index, List<String> names, List<String> unresolved) {
        List<Long> ids = new ArrayList<>();
        for (String name : names) {
            Long id = find(index, name);
            if (id == null) {
                unresolved.add(name);
            } else if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        // Collapse inner whitespace too: "Supplements/ Nutraceuticals" and
        // "Supplements / Nutraceuticals" are the same master in practice.
        String n = value.trim().replaceAll("\\s+", " ").toLowerCase();
        return n.isEmpty() ? null : n;
    }
}
