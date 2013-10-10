package bspkrs.ingameinfo.fml;

import java.util.EnumSet;

import bspkrs.ingameinfo.IngameInfo;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class IGIRenderTicker implements ITickHandler
{
    private EnumSet<TickType> tickTypes = EnumSet.noneOf(TickType.class);
    
    public IGIRenderTicker(EnumSet<TickType> tickTypes)
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
        
        return IngameInfo.onTickInGame();
    }
    
    @Override
    public EnumSet<TickType> ticks()
    {
        return tickTypes;
    }
    
    @Override
    public String getLabel()
    {
        return "IGIRenderTicker";
    }
}
