package livecheck;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

// test only, PatchLib 1.0.3. typeName targets so the same jar runs on 2.0.c, a class it lacks just never installs
public final class Timers {
    private Timers() {
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.plugins.ModPlugin"))
    public static final class GameLoad {
        @Before(target = @MethodMatch(methodName = "onGameLoad"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "onGameLoad"))
        public static void after(AfterContext context) { Perf.stop(context, "ModPlugin.onGameLoad"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.memory.ImageDataMemory"))
    public static final class Refresh {
        @Before(target = @MethodMatch(methodName = "forceRefresh"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "forceRefresh"))
        public static void after(AfterContext context) { Perf.stop(context, "ImageDataMemory.forceRefresh"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.helper.ImageHandler"))
    public static final class Handler {
        @Before(target = @MethodMatch(methodName = "addPresetImagesToMarkets"))
        public static void presetBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "addPresetImagesToMarkets"))
        public static void presetAfter(AfterContext context) { Perf.stop(context, "ImageHandler.addPresetImagesToMarkets"); }

        @Before(target = @MethodMatch(methodName = "addRandomImagesToMarketsWithoutImage"))
        public static void randomBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "addRandomImagesToMarketsWithoutImage"))
        public static void randomAfter(AfterContext context) { Perf.stop(context, "ImageHandler.addRandomImagesToMarketsWithoutImage"); }

        @Before(target = @MethodMatch(methodName = "loadImagesForCurrentLocation"))
        public static void locationBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "loadImagesForCurrentLocation"))
        public static void locationAfter(AfterContext context) { Perf.stop(context, "ImageHandler.loadImagesForCurrentLocation"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.loading.Importer"))
    public static final class Files {
        @Before(target = @MethodMatch(methodName = "loadImage"))
        public static void loadBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "loadImage"))
        public static void loadAfter(AfterContext context) { Perf.stop(context, "Importer.loadImage"); }

        @Before(target = @MethodMatch(methodName = "unloadImage"))
        public static void unloadBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "unloadImage"))
        public static void unloadAfter(AfterContext context) { Perf.stop(context, "Importer.unloadImage"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.listeners.ImageOnJumpLoader"))
    public static final class Jump {
        @Before(target = @MethodMatch(methodName = "reportFleetJumped"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "reportFleetJumped"))
        public static void after(AfterContext context) { Perf.stop(context, "ImageOnJumpLoader.reportFleetJumped"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.listeners.UnusedImageUnloader"))
    public static final class Unloader {
        @Before(target = @MethodMatch(methodName = "reportEconomyTick"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "reportEconomyTick"))
        public static void after(AfterContext context) { Perf.stop(context, "UnusedImageUnloader.reportEconomyTick"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.rules.ShowDefaultVisualWithSelector"))
    public static final class Visual {
        @Before(target = @MethodMatch(methodName = "execute"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "execute"))
        public static void after(AfterContext context) { Perf.stop(context, "ShowDefaultVisualWithSelector.execute"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.impl.ImageSelectorPanel"))
    public static final class Selector {
        @Before(target = @MethodMatch(methodName = "showPanel"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "showPanel"))
        public static void after(AfterContext context) { Perf.stop(context, "ImageSelectorPanel.showPanel"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.impl.DeleteDescriptionDialog"))
    public static final class DeletePopup {
        @Before(target = @MethodMatch(methodName = "createCustomDialog"))
        public static void open(BeforeContext context) {
            Perf.popupPanel = (CustomPanelAPI) context.getArg(0);
            Perf.popupCallback = (CustomDialogDelegate.CustomDialogCallback) context.getArg(1);
            Perf.popupResult = null;
        }

        @After(target = @MethodMatch(methodName = "customDialogConfirm"))
        public static void confirm(AfterContext context) { Perf.popupResult = "confirm"; }
    }

    // the popup inherits cancel from the api base class, so that is where it is declared
    @Patch(target = @ClassMatch(typeName = "com.fs.starfarer.api.campaign.BaseCustomDialogDelegate"))
    public static final class PopupCancel {
        @After(target = @MethodMatch(methodName = "customDialogCancel"))
        public static void cancel(AfterContext context) {
            if (context.getSelf().getClass().getName().endsWith(".DeleteDescriptionDialog")) Perf.popupResult = "cancel";
        }
    }

    // Perf.holdImageLoads keeps the tiles on their spinner for a capture
    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.panel.ImageListPanelPlugin"))
    public static final class ImageLoads {
        @Before(target = @MethodMatch(methodName = "advance"))
        public static void before(BeforeContext context) {
            if (Perf.holdImageLoads) context.skipOriginal(null);
            else Perf.start(context);
        }

        @After(target = @MethodMatch(methodName = "advance"))
        public static void after(AfterContext context) { Perf.stop(context, "ImageListPanelPlugin.advance"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.impl.TextChangerPanel"))
    public static final class Editor {
        @Before(target = @MethodMatch(methodName = "showPanel"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "showPanel"))
        public static void after(AfterContext context) { Perf.stop(context, "TextChangerPanel.showPanel"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin"))
    public static final class Buttons {
        @Before(target = @MethodMatch(methodName = "advance"))
        public static void before(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "advance"))
        public static void after(AfterContext context) { Perf.stop(context, "InteractionDialogCustomPanelPlugin.advance"); }
    }

    @Patch(target = @ClassMatch(typeName = "illustratedEntities.dialogue.panel.MultilineTextField"))
    public static final class Field {
        @Before(target = @MethodMatch(methodName = "advance"))
        public static void advance(BeforeContext context) { Perf.field = context.getSelf(); }

        @Before(target = @MethodMatch(methodName = "render"))
        public static void renderBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "render"))
        public static void renderAfter(AfterContext context) {
            Perf.stop(context, "MultilineTextField.render");
            if (Perf.readText) Perf.readText(context.getSelf(), context.<PositionAPI>getField("position").get());
        }

        @Before(target = @MethodMatch(methodName = "processInput"))
        public static void inputBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "processInput"))
        public static void inputAfter(AfterContext context) { Perf.stop(context, "MultilineTextField.processInput"); }

        @Before(target = @MethodMatch(methodName = "relayout"))
        public static void layoutBefore(BeforeContext context) { Perf.start(context); }

        @After(target = @MethodMatch(methodName = "relayout"))
        public static void layoutAfter(AfterContext context) { Perf.stop(context, "MultilineTextField.relayout"); }
    }

    // waiting for the driver happens in here, Perf.frame takes it off the frame time
    @Patch(target = @ClassMatch(typeName = "org.lwjgl.opengl.Display"))
    public static final class DisplayWait {
        private static void in() {
            if (Perf.depth++ == 0) Perf.displayStart = System.nanoTime();
        }

        private static void out() {
            if (Perf.depth > 0 && --Perf.depth == 0) Perf.displayNanos += System.nanoTime() - Perf.displayStart;
        }

        @Before(target = @MethodMatch(methodName = "update"))
        public static void updateBefore(BeforeContext context) { in(); }

        @After(target = @MethodMatch(methodName = "update"))
        public static void updateAfter(AfterContext context) { out(); }

        @Before(target = @MethodMatch(methodName = "sync"))
        public static void syncBefore(BeforeContext context) { in(); }

        @After(target = @MethodMatch(methodName = "sync"))
        public static void syncAfter(AfterContext context) { out(); }
    }
}
