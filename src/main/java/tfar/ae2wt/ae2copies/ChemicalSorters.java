package tfar.ae2wt.ae2copies;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import tfar.ae2wt.util.ChemicalHelper;

import java.util.Comparator;

public class ChemicalSorters {

    @SuppressWarnings("unchecked")
    public static Comparator<Object> getComparator(SortOrder order, SortDir dir) {

        switch (order) {
            case NAME:
            default:
                return dir == SortDir.ASCENDING ?
                        Comparator.comparing(ChemicalHelper::getChemicalDisplayName, String.CASE_INSENSITIVE_ORDER) :
                        Comparator.comparing(ChemicalHelper::getChemicalDisplayName, String.CASE_INSENSITIVE_ORDER).reversed();
            case AMOUNT:
                return dir == SortDir.ASCENDING ?
                        Comparator.comparingLong(ChemicalHelper::getChemicalStackSize) :
                        Comparator.comparingLong(ChemicalHelper::getChemicalStackSize).reversed();
            case MOD:
                return dir == SortDir.ASCENDING ?
                        Comparator.comparing(ChemicalHelper::getChemicalModId, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ChemicalHelper::getChemicalDisplayName, String.CASE_INSENSITIVE_ORDER) :
                        Comparator.comparing(ChemicalHelper::getChemicalModId, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ChemicalHelper::getChemicalDisplayName, String.CASE_INSENSITIVE_ORDER).reversed();
        }
    }
}