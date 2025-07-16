class JenkinsUtils implements Serializable {
    def steps

    JenkinsUtils(steps) {
        this.steps = steps
    }

    def getServer(ambiente, tipoAcceso) {
        def serverMap = [
            'drs'     : { -> steps.env.SERVER_DRS },
            'internal': { -> steps.env.SERVER_INTERNAL },
            'external': { -> steps.env.SERVER_EXTERNAL }
        ]
        if (ambiente?.toLowerCase() == 'drs') return serverMap['drs']()
        def key = tipoAcceso?.toLowerCase()
        return serverMap.get(key, serverMap['internal'])()
    }

    def getOcLoginCmd(server, token) {
        return "oc login --insecure-skip-tls-verify --server=${server} --token=${token}"
    }

    def getOcLogoutCmd() {
        return "oc logout"
    }

    def getChmodCmd(file) {
        return "chmod 644 ${file}"
    }

    def getCleanVarsCmd(nombre, resourceType) {
        return """oc set env deployment/${nombre} --list | grep ${resourceType} | awk '{print  \$2}' | while read VR; do echo \$VR; oc set env deployment/${nombre} \$VR-; done"""
    }

    def getSetFromCmd(nombre, resourceType) {
        return "oc set env deployment/${nombre} --from=${resourceType}/${nombre}"
    }

    def getScaleCmd(nombre, replicas) {
        return "oc scale deployment/${nombre} --replicas=${replicas}"
    }

    def getResourceCmd(resourceType, nombre, namespace, yamlFile) {
        def resourceTypeMap = getResourceTypeMap()
        return resourceTypeMap[resourceType]?.getResourceCmd?.call(nombre, namespace, yamlFile)
    }

    def getBackupCmd(resourceType, nombre, namespace, backupFile) {
        def resourceTypeMap = getResourceTypeMap()
        return resourceTypeMap[resourceType]?.getBackupCmd?.call(nombre, namespace, backupFile)
    }

    def existsCmd(resourceType, nombre, namespace) {
        def resourceTypeMap = getResourceTypeMap()
        return resourceTypeMap[resourceType]?.existsCmd?.call(nombre, namespace)
    }

    def deleteCmd(resourceType, nombre, namespace) {
        def resourceTypeMap = getResourceTypeMap()
        return resourceTypeMap[resourceType]?.deleteCmd?.call(nombre, namespace)
    }

    private def getResourceTypeMap() {
        return [
            'certificados': [
                getResourceCmd: { nombre, namespace, yamlFile ->
                    steps.unstash 'FileCer.zip'
                    steps.sh 'unzip -o FileCer.zip -d certs'
                    def files = steps.sh(script: "ls certs", returnStdout: true).trim().split("\n")
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
    }
}
