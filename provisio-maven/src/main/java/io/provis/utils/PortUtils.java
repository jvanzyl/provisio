package io.provis.utils;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtils
{

    public static int getFreePort()
    {
        try
        {
            final ServerSocket server = new ServerSocket( 0 );
            try
            {
                return server.getLocalPort();
            }
            finally
            {
                try
                {
                    server.close();
                }
                catch ( final IOException e )
                {
                    // ignored
                }
            }
        }
        catch ( final IOException e )
        {
            return 0;
        }
    }

}
