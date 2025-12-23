package lu.kolja.expandedae.mixin.patternprovider;

import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import lu.kolja.expandedae.helper.pattern.ExpandedAE$PatternProviderTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTarget$1")
public abstract class MixinPatternProviderTargetImpl implements ExpandedAE$PatternProviderTarget {

    @Final @Shadow MEStorage val$storage;

    @Override
    public boolean expandedae$onlyHasPatternInput(Set<AEKey> patternInputs) {
        for (var stack : val$storage.getAvailableStacks()) {
            if (patternInputs.contains(stack.getKey().dropSecondary())) continue;
            return false;
        }
        return true;
    }

    @Override
    public MEStorage expandedae$getStorage() {
        return val$storage;
    }

}
