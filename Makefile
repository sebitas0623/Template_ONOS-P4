ONOS_VERSION = 2.1.0
ONOS_MD5 = 6ca21242cf837a726cfbcc637107026b
ONOS_URL = http://repo1.maven.org/maven2/org/onosproject/onos-releases/$(ONOS_VERSION)/onos-$(ONOS_VERSION).tar.gz
ONOS_TAR_PATH = ~/onos.tar.gz
APP_OAR = app/target/template-1.0-SNAPSHOT.oar
OCI = 192.168.1.189

p4:
	cd p4src && make build

onos-cli:
	onos

app-build: p4
	$(info ************ BUILDING ONOS APP ************)
	cd app && mvn clean package

$(APP_OAR):
	$(error Missing app binary, run 'make app-build' first)

app-reload: $(APP_OAR)
	$(info ************ RELOADING ONOS APP ************)
	onos-app $(OCI) reinstall! app/target/template-1.0-SNAPSHOT.oar

test-all:
	$(info ************ RUNNING ALL PTF TESTS ************)
	cd ptf && make all

reset:
	-cd ~ && ./kill_onos.sh
	-cd p4src && make clean
	-cd ptf && make clean
	-sudo rm -rf app/target
	-sudo mn -c
	-sudo rm -rf /tmp/bmv2-*


