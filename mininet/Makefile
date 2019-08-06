ONOS_VERSION = 2.1.0
ONOS_MD5 = 6ca21242cf837a726cfbcc637107026b
ONOS_URL = http://repo1.maven.org/maven2/org/onosproject/onos-releases/$(ONOS_VERSION)/onos-$(ONOS_VERSION).tar.gz
ONOS_TAR_PATH = ~/onos.tar.gz
APP_OAR = app/target/srv6-tutorial-1.0-SNAPSHOT.oar
OCI = 192.168.1.83

onos-cli:
	onos

topo:
	$(info ************ STARTING MININET TOPOLOGY ************)
	-sudo rm -rf bmv2-*	
	-sudo mn -c	
	-sudo -E python topo.py --onos-ip ${OCI}

netcfg:
	$(info ************ PUSHING NETCFG TO ONOS ************)
	-onos-netcfg $(OCI) bmv2-s1-netcfg.json 
	-onos-netcfg $(OCI) bmv2-s2-netcfg.json
	-onos-netcfg $(OCI) bmv2-s3-netcfg.json

reset:
	-sudo mn -c
	-sudo rm -rf /tmp/bmv2-*


