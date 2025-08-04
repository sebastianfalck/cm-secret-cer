<h1>📘 Pipeline Jenkins para Autosoporte de Microservicios en OpenShift</h1>

<p>Este pipeline permite a los desarrolladores gestionar e inspeccionar recursos de Kubernetes/OpenShift (como <code>configmaps</code>, <code>secrets</code> y <code>certificados</code>) asociados a su microservicio, de forma controlada, segura y autogestionada.</p>

<hr/>

<h2>🔧 Clase Utilitaria: <code>JenkinsUtils</code></h2>
<p>Clase auxiliar que proporciona métodos reutilizables para:</p>
<ul>
  <li>Seleccionar el servidor OpenShift correcto según el ambiente o uso (interno, externo o DRS).</li>
  <li>Generar comandos <code>oc</code> para login/logout, escalado, creación, backup, eliminación y extracción de recursos.</li>
  <li>Manejar diferencias sintácticas entre <code>configmap</code>, <code>secret</code> y <code>certificados</code>, con una interfaz común.</li>
</ul>

<h2>🧾 Parámetros del Pipeline</h2>
<table>
  <tr><th>Parámetro</th><th>Tipo</th><th>Descripción</th></tr>
  <tr><td><b>NOMBRE</b></td><td>string</td><td>Nombre del microservicio según el archivo CSV.</td></tr>
  <tr><td><b>AMBIENTE</b></td><td>choice (dev, uat, prd, drs)</td><td>Ambiente de destino en OpenShift.</td></tr>
  <tr><td><b>RESOURCE_TYPE</b></td><td>choice (configmap, secret, certificados)</td><td>Tipo de recurso a gestionar.</td></tr>
  <tr><td><b>KEY_VALUE_PAIRS</b></td><td>texto</td><td>Contenido del archivo <code>datos.txt</code> (clave=valor).</td></tr>
  <tr><td><b>FileCer.zip</b></td><td>archivo</td><td>ZIP con certificados (solo para <code>certificados</code>).</td></tr>
</table>

<h2>🌍 Variables de Entorno</h2>
<table>
  <tr><th>Variable</th><th>Descripción</th></tr>
  <tr><td><b>REPO_URL</b></td><td>Repositorio Git con el archivo CSV de configuración.</td></tr>
  <tr><td><b>REPO_CREDENTIALS</b></td><td>ID de credencial Jenkins para Git.</td></tr>
  <tr><td><b>SERVER_INTERNAL</b></td><td>URL del clúster OpenShift interno.</td></tr>
  <tr><td><b>SERVER_EXTERNAL</b></td><td>URL del clúster OpenShift externo.</td></tr>
  <tr><td><b>SERVER_DRS</b></td><td>URL del clúster de recuperación (DRS).</td></tr>
</table>

<h2>🔄 Etapas del Pipeline</h2>

<h3>1. Validación del Ambiente por Rama</h3>
<p>Valida que el ambiente elegido (<code>AMBIENTE</code>) sea permitido según la rama actual de Git:</p>
<ul>
  <li>Rama <code>dev</code>: solo <code>dev</code> y <code>uat</code>.</li>
  <li>Rama <code>main</code>: todos los ambientes permitidos.</li>
</ul>

<h3>2. Clonación del Repositorio</h3>
<p>Obtiene el repositorio Git con el archivo <code>ProjectsJenkinsCardifCSV.csv</code>.</p>

<h3>3. Lectura y Parseo del CSV</h3>
<p>Lee el archivo CSV, encuentra la fila correspondiente al microservicio (<code>NOMBRE</code>) y la guarda como <code>fila_&lt;nombre&gt;.json</code>.</p>

<h3>4. Confirmación de País</h3>
<p>Valida que el país registrado para el microservicio en el CSV coincida con la carpeta padre del workspace de Jenkins. Si no coincide, falla.</p>

<h3>5. Generación de <code>datos.txt</code></h3>
<p>Solo si el recurso es <code>configmap</code> o <code>secret</code>, se crea un archivo <code>datos.txt</code> con el contenido clave=valor ingresado.</p>

<h3>6. Generación de YAML del Recurso</h3>
<p>Usa <code>oc create</code> en modo <code>--dry-run=client</code> para generar el YAML del recurso, como <code>configmap-&lt;nombre&gt;.yaml</code> o <code>certificados-&lt;nombre&gt;.yaml</code>.</p>

<h3>7. Backup del Recurso</h3>
<p>Realiza login en OpenShift y hace un backup del recurso actual con <code>oc get -o yaml</code>, guardándolo con timestamp en la ruta <code>/tmp/backup/&lt;país&gt;/&lt;proyecto&gt;/&lt;microservicio&gt;/&lt;AMBIENTE&gt;/&lt;tipo&gt;/</code>.</p>

<h3>8. Eliminación del Recurso (si existe)</h3>
<p>Verifica si el recurso existe en el clúster. Si existe, lo elimina con <code>oc delete</code>. Si no, continúa.</p>

<h3>9. Reaplicación del Recurso y Reinicio del Deployment</h3>
<p>Recrea el recurso con <code>oc apply</code>, lo extrae, escala el deployment a 0, limpia variables de entorno si aplica (<code>configmap</code> o <code>secret</code>), y escala nuevamente a 1.</p>

<hr/>

<h2>⚙️ Recursos Soportados</h2>
<p>El pipeline gestiona estos tres tipos de recursos:</p>
<ul>
  <li><code>configmap</code>: generado desde <code>datos.txt</code> con clave=valor.</li>
  <li><code>secret</code>: igual que configmap, pero en base64.</li>
  <li><code>certificados</code>: ZIP de archivos CER/KEY que se cargan como <code>generic secret</code>.</li>
</ul>

<h2>📂 Estructura del CSV</h2>
<p>El archivo <code>ProjectsJenkinsCardifCSV.csv</code> debe tener la siguiente estructura:</p>
<pre>
appName;country;usage;NameSpaceDev;TokenDev;NameSpaceUat;TokenUat;NameSpacePrd;TokenPrd;project
</pre>

<hr/>

<h2>📎 Buenas Prácticas</h2>
<ul>
  <li>El microservicio debe estar registrado en el CSV.</li>
  <li>Las variables de entorno solo se reinician si el recurso es <code>configmap</code> o <code>secret</code>.</li>
  <li>La ruta del backup incluye país, proyecto y nombre del microservicio.</li>
  <li>El pipeline realiza login/logout de forma segura con máscaras de token.</li>
</ul>

<hr/>

<h2>🔗 Recursos Relacionados</h2>
<ul>
  <li><a href="https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/developer-cli-commands.html" target="_blank">Documentación CLI de OpenShift</a></li>
  <li><a href="https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/" target="_blank">Guía ConfigMap Kubernetes</a></li>
  <li><a href="https://www.jenkins.io/doc/book/security/" target="_blank">Seguridad en Jenkins</a></li>
</ul>
