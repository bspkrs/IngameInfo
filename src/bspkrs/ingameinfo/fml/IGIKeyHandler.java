package bspkrs.ingameinfo.fml;

import java.util.EnumSet;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import bspkrs.ingameinfo.IngameInfo;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.TickType;

public class IGIKeyHandler extends KeyBindingRegistry.KeyHandler
{
    public IGIKeyHandler(KeyBinding[] keyBindings, boolean[] repeatings)
    {
        super(keyBindings, repeatings);
    }
    
    @Override
    public String getLabel()
    {
        return "IGIKeyHandler";
    }
    
    @Override
    public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat)
    {
        for (int i = 0; i < keyBindings.length; i++)
        {
            if (kb.keyCode == keyBindings[i].keyCode)
            {
                boolean state = (kb.keyCode < 0 ? Mouse.isButtonDown(kb.keyCode + 100) : Keyboard.isKeyDown(kb.keyCode));
                
                for (TickType tt : types)
                    if (state != keyDown[i] && tickEnd && tt == TickType.CLIENT)
                        IngameInfo.keyboardEvent(kb);
            }
            
        }
    }
    
    @Override
    public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd)
    {   
        
    }
    
    @Override
    public EnumSet<TickType> ticks()
    {
        return EnumSet.of(TickType.CLIENT);
    }
    
}
