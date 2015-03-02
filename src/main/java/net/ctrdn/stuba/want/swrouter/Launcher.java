package net.ctrdn.stuba.want.swrouter;

import net.ctrdn.stuba.want.swrouter.core.RouterController;


public class Launcher 
{
    public static void main( String[] args )
    {
        RouterController routerController = new RouterController();
        routerController.start();
    }
}
