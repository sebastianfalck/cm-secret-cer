# Jenkins Pipeline: OpenShift Microservice Resource Automation

Este documento describe la lógica y propósito de cada etapa (`stage`) del pipeline Jenkins definido en el archivo `Jenkinfile` para la gestión automatizada y segura de recursos OpenShift para microservicios.

---

## Stages del Pipeline

### 1. Checkout Project
- **Propósito:** Clona el repositorio del proyecto en la subcarpeta `project` usando la URL y credenciales definidas en variables de entorno.
- **Notas:** Evita sobrescribir archivos del workspace principal (como utilidades Groovy).

### 2. Preparar contexto
- **Propósito:**
  - Carga utilidades y funciones desde `jenkinsVars.groovy`.
  - Inicializa variables globales clave: namespace, tipo de recurso, nombre, ambiente, tipo de acceso, token y fecha.
- **Notas:** Permite que las siguientes etapas tengan acceso a utilidades y contexto adecuado.

### 3. Cargar variables del microservicio
- **Propósito:**
  - Lee la configuración del microservicio desde el archivo CSV (`project/ProjectsJenkinsCardifCSV.csv`).
  - Exporta cada columna como variable de entorno.
  - Si existe la columna `usage`, también la exporta como `TIPO_ACCESO` para determinar el tipo de acceso (interno/externo).
- **Notas:** Permite parametrizar el pipeline según la configuración específica de cada microservicio.

### 4. Crear archivo datos.txt
- **Propósito:** Crea un archivo `datos.txt` con el tipo de recurso (`RESOURCE_TYPE`).
- **Notas:** Puede ser usado como input para otras herramientas o para auditoría.

### 5. Mostrar datos.txt
- **Propósito:** Muestra el contenido de `datos.txt` en la consola Jenkins para verificación.

### 6. Generar YAML
- **Propósito:**
  - Realiza login seguro a OpenShift usando utilidades y token enmascarado.
  - Genera el archivo YAML correspondiente al recurso solicitado (configmap, secret, certificados).
  - Utiliza funciones utilitarias para construir y ejecutar los comandos `oc`.
  - Realiza logout seguro al finalizar.
- **Notas:** El nombre del archivo YAML varía según el tipo de recurso.

### 7. Backup recurso actual en OpenShift
- **Propósito:**
  - Realiza backup del recurso actual en OpenShift antes de modificarlo/eliminarlo.
  - Guarda el backup en la carpeta `tmp` con timestamp.
  - Aplica permisos adecuados al archivo backup.
  - Login/logout seguro a OpenShift.
- **Notas:** Permite recuperación ante fallos o auditoría.

### 8. Eliminar recurso existente si aplica
- **Propósito:**
  - Verifica si el recurso existe en OpenShift.
  - Si existe, lo elimina usando los comandos utilitarios.
  - Login/logout seguro a OpenShift.
- **Notas:** Evita conflictos al crear recursos nuevos.

### 9. Modificación de deployment Yaml
- **Propósito:**
  - Login seguro a OpenShift.
  - Escala el deployment a 0 réplicas y luego a 1 para aplicar cambios.
  - Si el recurso no es `certificados`, limpia variables de entorno relacionadas y las reasigna desde el recurso correspondiente.
  - Logout seguro.
- **Notas:** Garantiza que los cambios de configuración se apliquen correctamente en el deployment.

### 10. Finalización del pipeline
- **Propósito:**
  - Muestra mensaje de éxito con detalles del microservicio, ambiente y tipo de recurso.
  - Realiza un escalado final del deployment a 0 y luego a 1 réplica para asegurar la aplicación de cambios.
  - Login/logout seguro a OpenShift.

---

## Notas adicionales
- **Seguridad:** Todos los comandos que involucran tokens o información sensible usan wrappers y utilidades para evitar fugas en logs.
- **Centralización:** Toda la lógica de comandos `oc` y shell está centralizada en `jenkinsVars.groovy` para facilitar el mantenimiento y la reutilización.
- **Flexibilidad:** El pipeline es completamente parametrizable y no tiene valores hardcodeados salvo los mínimos necesarios para el checkout.

---

> Última actualización: 2025-07-11
