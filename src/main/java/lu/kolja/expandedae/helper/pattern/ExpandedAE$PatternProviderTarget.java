package lu.kolja.expandedae.helper.pattern;

import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.helpers.patternprovider.PatternProviderTarget;

import java.util.Set;

public interface ExpandedAE$PatternProviderTarget extends PatternProviderTarget {

    boolean expandedae$onlyHasPatternInput(Set<AEKey> patternInputs);

    MEStorage expandedae$getStorage();
}
