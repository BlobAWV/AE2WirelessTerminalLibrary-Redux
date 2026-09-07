package tfar.ae2wt.ae2copies;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import tfar.ae2wt.util.ChemicalHelper;

import java.util.Comparator;
import java.util.regex.Pattern;

public class ChemicalRepo extends Repo {
    public ChemicalRepo(IScrollSource src, ISortSource sortSrc) {
        super(src, sortSrc);
    }

    @Override
    protected boolean matchesSearch(SearchMode searchMode, Pattern searchPattern, IAEStack stack) {
        if (!ChemicalHelper.CHEMICALS_PRESENT) return false;
        String displayName;
        if (searchMode == SearchMode.MOD) {
            displayName = ChemicalHelper.getChemicalDisplayName(stack);
            return searchPattern.matcher(displayName).find();
        } else {
            displayName = ChemicalHelper.getChemicalDisplayName(stack);
            return searchPattern.matcher(displayName).find();
        }
    }

    @Override
    protected Comparator<? super Object> getComparator(SortOrder sortBy, SortDir sortDir) {
        return ChemicalSorters.getComparator(sortBy, sortDir);
    }
}