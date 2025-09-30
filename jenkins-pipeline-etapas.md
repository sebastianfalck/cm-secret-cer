# Jenkins pipeline - Etapas e instrucciones de uso

Este documento describe las etapas del `Jenkinsfile` del repositorio `cm-secret-cer` y proporciona instrucciones de uso y pruebas para ejecutar el pipeline correctamente.

## Resumen del flujo

El pipeline sigue este flujo general:

1. Validar que el ambiente (`AMBIENTE`) sea permitido para la rama actual.
2. Hacer checkout del repositorio con la configuración de proyectos (`ProjectsJenkinsCardifCSV.csv`).
3. Leer la fila de configuración del microservicio (por `NOMBRE`) y validar el país/namespace.
4. Recuperar el adjunto `var.zip` desde el stash y descomprimirlo en `unzipped/`.
5. Detectar automáticamente los tipos de recursos presentes: `certificados` (si existe `Certificados.zip`), `configmap` (si existe `Configmap.txt`) y `secret` (si existe `Secrets.txt`).
6. Generar `datos.txt` si hay `configmap` o `secret`.
7. Generar los YAMLs correspondientes a cada tipo detectado y aplicarlos en OpenShift.
8. Realizar backup de los recursos detectados.
9. Eliminar recursos existentes y recrearlos aplicando los YAMLs, reiniciando despliegues.
10. Extraer los recursos aplicados.

## Requisitos previos

- Jenkins con el plugin Pipeline (declarative). 
- Credenciales configuradas: `github-credentials` para checkout y que los tokens (por ambiente) estén definidos en el CSV (`ProjectsJenkinsCardifCSV.csv`).
- Plugin `Mask Passwords` (o equivalente) para envolver tokens.
- Un job que prepare y suba el stash `var.zip` antes de ejecutar este pipeline. El stash debe llamarse `var.zip`.

## Parámetros del job

- `NOMBRE` (string): nombre del microservicio (clave `appName` en el CSV).
- `AMBIENTE` (choice): `dev`, `uat`, `prd`, `drs`.
- `KEY_VALUE_PAIRS` (text): contenidos para `datos.txt` (solo requerido si el adjunto contiene `Configmap.txt` o `Secrets.txt`).

Nota: El parámetro `RESOURCE_TYPE` fue eliminado; el pipeline detecta automáticamente los tipos a partir del adjunto.

## Estructura esperada del `var.zip`

El `var.zip` (stash) puede contener una o varias de las siguientes entradas:

- `Certificados.zip` — archivo ZIP que contiene archivos de certificado.
- `Configmap.txt` — archivo con pares clave=valor para generar un ConfigMap (si existe, `KEY_VALUE_PAIRS` también puede usarse para generar `datos.txt`).
- `Secrets.txt` — archivo con pares clave=valor para generar un Secret (igual que Configmap respecto a `datos.txt`).

Ejemplos:
- Solo certificados: `var.zip` contiene `Certificados.zip`.
- Configmap + secret: `var.zip` contiene `Configmap.txt` y `Secrets.txt`.
- All-in-one: `var.zip` contiene `Certificados.zip`, `Configmap.txt` y `Secrets.txt`.

## Cómo preparar y subir el stash (ejemplo)

En una etapa previa (o en otro job) debes crear y subir el stash con nombre `var.zip`. Un ejemplo en una pipeline que corre antes (Groovy declarative) sería:

```groovy
// Empaquetar y stashear
sh 'zip -r var.zip Certificados.zip Configmap.txt Secrets.txt'
stash name: 'var.zip', includes: 'var.zip'
```

Asegúrate de que el job que genera el stash y el job que lo consume corran en el mismo nodo o que utilices `stash/unstash` correctamente entre etapas del mismo pipeline.

## Ejecución y pruebas

1. Subir el stash `var.zip` siguiendo el ejemplo anterior.
2. Ejecutar el job con los parámetros `NOMBRE` y `AMBIENTE`.
3. Validar en los logs que aparece el mensaje: `Tipos detectados: ...` y que los tipos reportados son los esperados.
4. Verificar que se generan los YAMLs (por ejemplo: `certificados-<NOMBRE>-file.yaml`, `configmap-<NOMBRE>.yaml`, `secret-<NOMBRE>.yaml`).
5. Verificar en OpenShift (o en logs) que los `oc apply` realizan las operaciones esperadas.

## Comprobaciones recomendadas

- Prueba con cada combinación de contenido del `var.zip`:
  - Solo `Certificados.zip`.
  - `Certificados.zip` + `Configmap.txt`.
  - `Configmap.txt` + `Secrets.txt`.
  - Ninguno (el pipeline fallará con error claro).

- Valida que `datos.txt` solo se genere cuando hay `configmap` o `secret`.
- Revisa el directorio `unzipped/` en los logs del job para confirmar la extracción.

## Opciones extra y mejoras sugeridas

- Modo dry-run: puedo añadir un parámetro `DRY_RUN` para que el pipeline genere YAMLs sin aplicarlos (útil para validación).
- Retries en comandos `oc`: agregar reintentos con backoff para comandos `oc` y `sh` que tocan la red.
- Validaciones adicionales: comprobar versiones, permisos y que los tokens en el CSV existan antes de intentar login.

---

Si quieres, agrego ahora la opción `DRY_RUN` y el parámetro `VERBOSE` para más logs. ¿Lo agrego y lo dejo como opción por defecto `true` (no aplica cambios) o `false` (aplica cambios)?