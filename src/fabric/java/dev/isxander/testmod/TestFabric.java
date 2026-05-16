package dev.isxander.testmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.launch.MixinBootstrap;


public class TestFabric {
    void test() {
        new TestCommon().test();
        Minecraft.getInstance();
        FabricLoader.getInstance();
        System.out.println(MixinBootstrap.VERSION);
    }
}
