from fabric import Connection
from fabric import task                                                                                  
import os
cn = Connection('evilgod@127.0.0.1')
# Finally my friends ... Python.

NUMBER_OF_REPLICAS = 3
#BASE_PORT = 19999

@task(hosts="Debian")
def deploy(c):
    for i in range(1,NUMBER_OF_REPLICAS):
        command = 'mvn exec:java'
        args = ' -Dexec.args="%s"' % i
        if os.fork() == 0:
            c.local(command + args)
            os._exit(0)
    input("Enter to kill...")
