package bspkrs.ingameinfo.fml;

import java.util.EnumSet;

import bspkrs.fml.util.bspkrsCoreProxy;
import bspkrs.ingameinfo.IngameInfo;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class IGIGameTicker implements ITickHandler
{
    private EnumSet<TickType> tickTypes        = EnumSet.noneOf(TickType.class);
    
    private boolean           allowUpdateCheck = bspkrsCoreProxy.instance.allowUpdateCheck;
    
    public IGIGameTicker(EnumSet<TickType> tickTypes)
    {
        this.tickTypes = tickTypes;
    }
    
    @Override
    public void tickStart(EnumSet<TickType> tickTypes, Object... tickData)
    {
        tick(tickTypes, true);
    }
    
    @Override
    public void tickEnd(EnumSet<TickType> tickTypes, Object... tickData)
    {
        tick(tickTypes, false);
    }
    
    private void tick(EnumSet<TickType> tickTypes, boolean isStart)
    {
        for (TickType tickType : tickTypes)
        {
            if (!onTick(tickType, isStart))
            {
                this.tickTypes.remove(tickType);
                this.tickTypes.removeAll(tickType.partnerTicks());
            }
        }
    }
    
    public boolean onTick(TickType tick, boolean isStart)
    {
        if (isStart)
        {
            return true;
        }
        
        if (allowUpdateCheck && IngameInfo.mc != null && IngameInfo.mc.thePlayer != null)
        {
            if (IngameInfoMod.versionChecker != null)
                if (!IngameInfoMod.versionChecker.isCurrentVersionBySubStringAsFloatNewer(IngameInfoMod.metadata.version.length() - 2, IngameInfoMod.metadata.version.length()))
                    for (String msg : IngameInfoMod.versionChecker.getInGameMessage())
                        IngameInfo.mc.thePlayer.addChatMessage(msg);
            
            allowUpdateCheck = false;
        }
        
        return false; //IngameInfo.onTickInGame();
    }
    
    @Override
    public EnumSet<TickType> ticks()
    {
        return tickTypes;
    }
    
    @Override
    public String getLabel()
    {
        return "IGIGameTicker";
    }
    
}
