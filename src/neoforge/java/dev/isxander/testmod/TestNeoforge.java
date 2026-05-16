package dev.isxander.testmod;

import net.fabricmc.mappingio.MappingFlag;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;

@Mod("testmod")
public class TestNeoforge {
    void test() {
        new TestCommon().test();
        NeoForge.EVENT_BUS.register(this);
        Minecraft.getInstance();
        MappingFlag.values();
    }
}
