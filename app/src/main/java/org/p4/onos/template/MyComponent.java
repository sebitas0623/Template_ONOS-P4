/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.p4.onos.template;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.*;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.pi.model.PiCounterType.INDIRECT;

/**
 * App component that configures devices to provide L2 bridging capabilities.
 */
@Component(
        immediate = true,
        enabled = true,
        service = MyComponent.class
)
public class MyComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int DEFAULT_BROADCAST_GROUP_ID = 255;

    private ApplicationId appId;

    //--------------------------------------------------------------------------
    // ONOS CORE SERVICE BINDING
    //
    // These variables are set by the Karaf runtime environment before calling
    // the activate() method.
    //--------------------------------------------------------------------------

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MainComponent mainComponent;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private P4RuntimeController p4RuntimeController;



    private final DeviceListener deviceListener = new MyComponent.InternalDeviceListener();
    Map<String, DeviceId> swIDs = new HashMap<String, DeviceId>();

    private static final long DEFAULT_P4_DEVICE_ID = 1;
    private PiCounterId INGRESS_COUNTER_ID = PiCounterId.of("c_ingress.rx_port_counter");
    private PiCounterId EGRESS_COUNTER_ID = PiCounterId.of("c_ingress.tx_port_counter");

    //--------------------------------------------------------------------------
    // COMPONENT ACTIVATION.
    //
    // When loading/unloading the app the Karaf runtime environment will call
    // activate()/deactivate().
    //--------------------------------------------------------------------------

    @Activate
    protected void activate() {
        appId = mainComponent.getAppId();
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }


    public Collection<PortStatistics> p4statisticsSW1(){
        if (swIDs.containsKey("s1")){
            PiPipeconf piPipeconf = piPipeconfService.getPipeconf(AppConstants.PIPECONF_ID).get();

            Collection<PortStatistics> conts = P4Counters(swIDs.get("s1"),piPipeconf);

            /*PiCounterModel countIngress = piPipeconf.pipelineModel().counter(INGRESS_COUNTER_ID).get();
            PiCounterModel countEgress = piPipeconf.pipelineModel().counter(EGRESS_COUNTER_ID).get();
            PiCounterModel.Unit counttype = countIngress.unit();
            long countsize = countIngress.size();*/

            return conts;
        }else{
            log.info("El switch S1 no está instanciado aún");
            return null;
        }
    }


    public Collection<PortStatistics> P4Counters(DeviceId deviceId, PiPipeconf piPipeconf){
        //se instancia el cliente p4Runtime para interactuar con los servidores (Switches)
        P4RuntimeClient cliente = p4RuntimeController.get(deviceId);
        // Prepare PortStatistics objects to return, one per port of this device.
        Map<Long, DefaultPortStatistics.Builder> portStatBuilders = Maps.newHashMap();
        deviceService
                .getPorts(deviceId)
                .forEach(p -> portStatBuilders.put(p.number().toLong(),
                        DefaultPortStatistics.builder()
                                .setPort(p.number())
                                .setDeviceId(deviceId)));
        // Generate the counter cell IDs.
        Set<PiCounterCellId> counterCellIds = Sets.newHashSet();
        portStatBuilders.keySet().forEach(p -> {
            // Counter cell/index = port number.
            counterCellIds.add(PiCounterCellId.ofIndirect(INGRESS_COUNTER_ID, p));
            counterCellIds.add(PiCounterCellId.ofIndirect(EGRESS_COUNTER_ID, p));
        });
        Set<PiCounterCellHandle> counterCellHandles = counterCellIds.stream()
                .map(id -> PiCounterCellHandle.of(deviceId, id))
                .collect(Collectors.toSet());
        // Query the device.
        Collection<PiCounterCell> counterEntryResponse = cliente.read(
                DEFAULT_P4_DEVICE_ID, piPipeconf)
                .handles(counterCellHandles).submitSync()
                .all(PiCounterCell.class);

        //log.info("{}",counterEntryResponse);

        // Process response.
        counterEntryResponse.forEach(counterCell -> {
            if (counterCell.cellId().counterType() != INDIRECT) {
                log.warn("Invalid counter data type {}, skipping", counterCell.cellId().counterType());
                return;
            }
            if (!portStatBuilders.containsKey(counterCell.cellId().index())) {
                log.warn("Unrecognized counter index {}, skipping", counterCell);
                return;
            }
            DefaultPortStatistics.Builder statsBuilder = portStatBuilders.get(counterCell.cellId().index());
            if (counterCell.cellId().counterId().equals(INGRESS_COUNTER_ID)) {
                statsBuilder.setPacketsReceived(counterCell.data().packets());
                statsBuilder.setBytesReceived(counterCell.data().bytes());
            } else if (counterCell.cellId().counterId().equals(EGRESS_COUNTER_ID)) {
                statsBuilder.setPacketsSent(counterCell.data().packets());
                statsBuilder.setBytesSent(counterCell.data().bytes());
            } else {
                log.warn("Unrecognized counter ID {}, skipping", counterCell);
            }
        });
        return portStatBuilders
                .values()
                .stream()
                .map(DefaultPortStatistics.Builder::build)
                .collect(Collectors.toList());
    }


    public class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    break;
                default:
                    // Ignore other events.
                    return false;
            }
            // Process only if this controller instance is the master.
            final DeviceId deviceId = event.subject().id();
            return mastershipService.isLocalMaster(deviceId);
        }

        @Override
        public void event(DeviceEvent event) {
            final DeviceId deviceId = event.subject().id();
            if (deviceService.isAvailable(deviceId)) {
                // A P4Runtime device is considered available in ONOS when there
                // is a StreamChannel session open and the pipeline
                // configuration has been set.
                mainComponent.getExecutorService().execute(() -> {
                    log.info("{} event! deviceId={}", event.type(), deviceId);
                    storeIdSwitches(deviceId);
                });
            }
        }

    }

    public void storeIdSwitches(DeviceId deviceId){
        String id = deviceId.toString();
        String key = id.substring(id.length()-2, id.length());
        swIDs.put(key,deviceId);
    }

}
