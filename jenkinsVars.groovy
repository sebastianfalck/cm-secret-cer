def getChmodCmd = { file ->
    return "chmod 644 ${file}"
}
def getOcLogoutCmd = { ->
    return "oc logout"
}
// Comandos utilitarios para deployment
def getCleanVarsCmd = { nombre, resourceType ->
    return "oc set env deployment/${nombre} --list | grep ${resourceType} | awk '{print  \\$2}' | while read VR; do oc set env deployment/${nombre} $VR- > /dev/null 2>&1; done"
}

def getSetFromCmd = { nombre, resourceType ->
    return "oc set env deployment/${nombre} --from=${resourceType}/${nombre}"
}

def getScaleCmd = { nombre, replicas ->
    return "oc scale deployment/${nombre} --replicas=${replicas}"
}
def getOcLoginCmd(server, token) {
    return "oc login --insecure-skip-tls-verify --server=${server} --token=${token}"
}
def getResourceCmd(resourceType, nombre, namespace, yamlFile) {
def getBackupCmd(resourceType, nombre, namespace, backupFile) {
// Archivo de variables y utilidades para pipelines Jenkins

def resourceTypeMap = [
    'certificados': [
        getResourceCmd: { nombre, namespace, yamlFile ->
            unstash 'FileCer.zip'
            sh 'unzip -o FileCer.zip -d certs'
            def files = sh(script: "ls certs", returnStdout: true).trim().split("\n")
            def fromFileArgs = files.collect { f -> "--from-file=${f}=certs/${f}" }.join(' ')
            return "oc create secret generic ${nombre}-file ${fromFileArgs} -n ${namespace} -o yaml --dry-run=client > ${yamlFile}"
        },
        getBackupCmd: { nombre, namespace, backupFile ->
            return "oc get secret ${nombre}-file -n ${namespace} -o yaml > ${backupFile} || true"
        },
        existsCmd: { nombre, namespace -> "oc get secret ${nombre}-file -n ${namespace}" },
        deleteCmd: { nombre, namespace -> "oc delete secret ${nombre}-file -n ${namespace}" }
    ],
    'secret': [
        getResourceCmd: { nombre, namespace, yamlFile ->
            return "oc create secret generic ${nombre} --from-env-file=datos.txt -n ${namespace} -o yaml --dry-run=client > ${yamlFile}"
        },
        getBackupCmd: { nombre, namespace, backupFile ->
            return "oc get secret ${nombre} -n ${namespace} -o yaml > ${backupFile} || true"
        },
        existsCmd: { nombre, namespace -> "oc get secret ${nombre} -n ${namespace}" },
        deleteCmd: { nombre, namespace -> "oc delete secret ${nombre} -n ${namespace}" }
    ],
    'configmap': [
        getResourceCmd: { nombre, namespace, yamlFile ->
            return "oc create configmap ${nombre} --from-env-file=datos.txt -n ${namespace} -o yaml --dry-run=client > ${yamlFile}"
        },
        getBackupCmd: { nombre, namespace, backupFile ->
            return "oc get configmap ${nombre} -n ${namespace} -o yaml > ${backupFile} || true"
        },
        existsCmd: { nombre, namespace -> "oc get configmap ${nombre} -n ${namespace}" },
        deleteCmd: { nombre, namespace -> "oc delete configmap ${nombre} -n ${namespace}" }
    ]
]

def serverMap = [
    'drs'     : { env -> env.SERVER_DRS },
    'internal': { env -> env.SERVER_INTERNAL },
    'external': { env -> env.SERVER_EXTERNAL }
]

def getResourceCmd(resourceType, nombre, namespace, yamlFile) {
    return resourceTypeMap[resourceType]?.getResourceCmd?.call(nombre, namespace, yamlFile)
}

def getBackupCmd(resourceType, nombre, namespace, backupFile) {
    return resourceTypeMap[resourceType]?.getBackupCmd?.call(nombre, namespace, backupFile)
}

def getServer(ambiente, tipoAcceso, env) {
    // Si el ambiente es DRS, siempre usar el servidor DRS
    if (ambiente?.toLowerCase() == 'drs') {
        return serverMap['drs'](env)
    }
    // Para dev/uat/prd, usar el tipo de acceso (internal/external)
    def key = tipoAcceso?.toLowerCase()
    if (serverMap.containsKey(key)) {
        return serverMap[key](env)
    }
    // Si no coincide, usar internal por defecto
    return serverMap['internal'](env)
}
