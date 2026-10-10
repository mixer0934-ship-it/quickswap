package com.example.quickswap.mixin;

import com.example.quickswap.BookFiller;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lets BookFiller swap the pages of an outgoing "sign book" packet. */
@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {

    // require = 0: if this ever fails to apply, the mod still loads and only the book feature is lost.
    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true, require = 0)
    private Packet<?> quickswap$fillBooks(Packet<?> packet) {
        return BookFiller.maybeRewrite(packet);
    }
}
