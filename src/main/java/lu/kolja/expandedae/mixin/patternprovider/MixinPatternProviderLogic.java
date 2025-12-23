package lu.kolja.expandedae.mixin.patternprovider;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.ConfigManager;
import com.llamalad7.mixinextras.sugar.Local;
import lu.kolja.expandedae.definition.ExpItems;
import lu.kolja.expandedae.definition.ExpSettings;
import lu.kolja.expandedae.enums.ADDONS;
import lu.kolja.expandedae.enums.BlockingMode;
import lu.kolja.expandedae.helper.pattern.IPatternProviderLogic;
import lu.kolja.expandedae.helper.pattern.ExpandedAE$PatternProviderTarget;
import lu.kolja.expandedae.mixin.accessor.AccessorCraftingCpuLogic;
import lu.kolja.expandedae.mixin.accessor.AccessorExecutingCraftingJob;
import lu.kolja.expandedae.mixin.compat.advancedae.AAEAccessorAdvCraftingCPULogic;
import lu.kolja.expandedae.mixin.compat.advancedae.AAEAccessorExecutingCraftingJob;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(value = PatternProviderLogic.class)
public abstract class MixinPatternProviderLogic implements IUpgradeableObject, IPatternProviderLogic {
    @Unique
    private static final boolean AAE_LOADED = ADDONS.ADV.isLoaded();

    @Shadow @Final private IActionSource actionSource;

    @Unique
    private IUpgradeInventory expandedae$upgrades = UpgradeInventories.empty();

    @Shadow @Final private PatternProviderLogicHost host;

    @Shadow @Final private IManagedGridNode mainNode;

    @Shadow @Final private IConfigManager configManager;

    @Shadow @Final private Set<AEKey> patternInputs;

    @Shadow private int roundRobinIndex;

    @Shadow @Final private List<GenericStack> sendList;

    @Shadow @Final private List<IPatternDetails> patterns;

    @Shadow private Direction sendDirection;

    @Shadow public abstract LockCraftingMode getCraftingLockedReason();

    @Shadow protected abstract Set<Direction> getActiveSides();

    @Shadow protected abstract void onPushPatternSuccess(IPatternDetails pattern);

    @Shadow protected abstract boolean adapterAcceptsAll(PatternProviderTarget target, KeyCounter[] inputHolder);

    @Shadow protected abstract <T> void rearrangeRoundRobin(List<T> list);

    @Shadow public abstract boolean isBlocking();

    @Shadow protected abstract boolean sendStacksOut();

    @Shadow protected abstract void addToSendList(AEKey what, long amount);

    @Shadow public abstract @Nullable IGrid getGrid();

    @Unique
    private void eae_$onUpgradesChanged() {
        /*
        if (!eae_$upgrades.isInstalled(ExpItems.SMART_BLOCKING_CARD)) { //TODO: smart card unlocks extra blocking modes
            assert Minecraft.getInstance().screen != null;
            ((IPatternProvider) me)
            ((IBlockingMode) Minecraft.getInstance().screen).setVisible(false);
        } else {
            assert Minecraft.getInstance().screen != null;
            ((IBlockingMode) Minecraft.getInstance().screen).setVisible(true);
        }*/
        this.host.saveChanges();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.expandedae$upgrades;
    }

    /*
     * This only gets run in the non-AppFlux mixin, as it adds the upgrades value, which doesn't happen in the AppFlux mixin.
     */
    @Inject(
            method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",
            at = @At("TAIL")
    )
    private void eae_$initUpgrade(IManagedGridNode mainNode, PatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        expandedae$upgrades = UpgradeInventories.forMachine(host.getTerminalIcon().getItem(), 1, this::eae_$onUpgradesChanged);
    }

    @Inject(
            method = "writeToNBT",
            at = @At("TAIL")
    )
    private void eae_$saveUpgrade(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.expandedae$upgrades.writeToNBT(tag, "upgrades", registries);
    }

    @Inject(
            method = "readFromNBT",
            at = @At("TAIL")
    )
    private void eae_$loadUpgrade(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.expandedae$upgrades.readFromNBT(tag, "upgrades", registries);
    }

    @Inject(
            method = "addDrops",
            at = @At("TAIL")
    )
    private void eae_$dropUpgrade(List<ItemStack> drops, CallbackInfo ci) {
        for (var is : this.expandedae$upgrades) if (!is.isEmpty()) drops.add(is);
    }

    @Inject(
            method = "clearContent",
            at = @At("TAIL")
    )
    private void eae_$clearUpgrade(CallbackInfo ci) {
        this.expandedae$upgrades.clear();
    }

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V",
            at = @At("TAIL"),
            remap = false)
    private void PatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host,
                                      int patternInventorySize, CallbackInfo ci) {
        ((ConfigManager) configManager).registerSetting(ExpSettings.BLOCKING_MODE, BlockingMode.DEFAULT);
    }

    @Override
    public BlockingMode expandedae$getBlockingMode() {
        return configManager.getSetting(ExpSettings.BLOCKING_MODE);
    }

    @Inject(
            method = "pushPattern",
            cancellable = true,
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic$1PushTarget;target()Lappeng/helpers/patternprovider/PatternProviderTarget;"
            )
    )
    private void expandedae$pushPatternSwitch(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir, @Local PatternProviderTarget adapter, @Local Direction direction){
        ExpandedAE$PatternProviderTarget eaeAdapter = (ExpandedAE$PatternProviderTarget)adapter;

        switch (expandedae$getBlockingMode()) {
            case ALL -> {
                if ((!this.isBlocking() || eaeAdapter.expandedae$getStorage().getAvailableStacks().isEmpty()) && this.adapterAcceptsAll(eaeAdapter, inputHolder)) {
                    patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                        long inserted = adapter.insert(what, amount, Actionable.MODULATE);
                        if (inserted < amount) {
                            addToSendList(what, amount - inserted);
                        }
                    });
                    onPushPatternSuccess(patternDetails);
                    sendDirection = direction;
                    sendStacksOut();
                    ++roundRobinIndex;
                    cir.setReturnValue(true);
                }
            }
            case SMART -> {
                if ((!this.isBlocking() || eaeAdapter.expandedae$getStorage().getAvailableStacks().isEmpty() || eaeAdapter.expandedae$onlyHasPatternInput(this.patternInputs)) && this.adapterAcceptsAll(eaeAdapter, inputHolder)) {
                    patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                        long inserted = adapter.insert(what, amount, Actionable.MODULATE);
                        if (inserted < amount) {
                            addToSendList(what, amount - inserted);
                        }
                    });
                    onPushPatternSuccess(patternDetails);
                    sendDirection = direction;
                    sendStacksOut();
                    ++roundRobinIndex;
                    cir.setReturnValue(true);
                }
            }
            case DEFAULT -> {
                //Fallthrough.
            }
        }
    }

    @Inject(
            method = "pushPattern",
            at = @At("HEAD")
    )
    private void expandedae$onPushPatternSuccess(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        expandedae$tryAutoCompleteCraft(patternDetails);
    }

    @Unique
    private void expandedae$tryAutoCompleteCraft(IPatternDetails details) {
        if (!getUpgrades().isInstalled(ExpItems.AUTO_COMPLETE_CARD)) return;
        var cpus = getGrid().getCraftingService().getCpus();
        for (var cpu : cpus) {
            if (!cpu.isBusy()) continue;
            if (cpu instanceof CraftingCPUCluster cluster) {
                var task = ((AccessorExecutingCraftingJob) ((AccessorCraftingCpuLogic) cluster.craftingLogic).getJob()).getTasks().get(details);
                if (task != null && task.getValue() <= 1) {
                    cluster.cancelJob();
                    return;
                }
                continue;
            }
            if (!AAE_LOADED) continue;
            if (cpu instanceof AdvCraftingCPU advCpu) {
                var task = ((AAEAccessorExecutingCraftingJob) ((AAEAccessorAdvCraftingCPULogic) advCpu.craftingLogic).getJob()).getTasks().get(details);
                if (task != null && task.getValue() <= 1) {
                    advCpu.cancelJob();
                    return;
                }
            }
        }
    }
}