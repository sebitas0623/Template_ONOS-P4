package org.p4.onos.template.cli;

import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.device.PortStatistics;
import org.p4.onos.template.MyComponent;

import java.util.Collection;
import java.util.Iterator;


@Service
@Command(scope = "onos", name = "p4statistics",
        description = "Muestra el estado de los contadores de P4")
public class MyCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() throws Exception {
        MyComponent service = get(MyComponent.class);

        Collection<PortStatistics> s = service.p4statisticsSW1();

        if (s != null) {
            Iterator<PortStatistics> iterador = s.iterator();
            while (iterador.hasNext()) {
                print("-------------------------------------------");
                print(iterador.next().toString());
            }
        }

    }
}

