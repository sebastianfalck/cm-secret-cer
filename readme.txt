<!-- HTML for Confluence documentation -->
<h1>📘 Jenkins Pipeline for Resource Management & Microservice Self‑Service</h1>

<p>This pipeline enables developers to manage and inspect Kubernetes/OpenShift resources (configmap, secret, certificates) related to their microservice in a controlled, secure, self-service manner.</p>

<hr/>

<h2>🔧 Utility Class: <code>JenkinsUtils</code></h2>
<p>This helper class provides reusable methods to:</p>
<ul>
  <li>Select the correct OpenShift server based on environment or usage.</li>
  <li>Generate <code>oc</code> commands for login/logout, scaling, resource creation, backup, deletion, and extraction for three resource types: <code>configmap</code>, <code>secret</code>, and <code>certificados</code>.</li>
  <li>Handle the differences in command syntax per resource type with a consistent interface.</li>
</ul>

<h2>🔧 Pipeline Parameters</h2>
<table>
  <tr><th>Parameter</th><th>Type</th><th>Description</th></tr>
  <tr><td><b>NOMBRE</b></td><td>string</td><td>Microservice name as listed in the CSV config.</td></tr>
  <tr><td><b>AMBIENTE</b></td><td>choice (dev, uat, prd, drs)</td><td>Target OpenShift environment.</td></tr>
  <tr><td><b>RESOURCE_TYPE</b></td><td>choice (configmap, secret, certificados)</td><td>Type of Kubernetes/OpenShift resource to manage.</td></tr>
  <tr><td><b>KEY_VALUE_PAIRS</b></td><td>text</td><td>Content for <code>datos.txt</code> (used for configmap/secret creation).</td></tr>
  <tr><td><b>FileCer.zip</b></td><td>stashed file</td><td>ZIP file of certificates (only for <code>certificados</code> resource type).</td></tr>
</table>

<h2>🌍 Environment Variables</h2>
<table>
  <tr><th>Variable</th><th>Description</th></tr>
  <tr><td><b>REPO_URL</b></td><td>Git repository containing the CSV config file.</td></tr>
  <tr><td><b>REPO_CREDENTIALS</b></td><td>Jenkins credential ID for Git access.</td></tr>
  <tr><td><b>SERVER_INTERNAL</b></td><td>Internal OpenShift cluster URL.</td></tr>
  <tr><td><b>SERVER_EXTERNAL</b></td><td>External OpenShift cluster URL.</td></tr>
  <tr><td><b>SERVER_DRS</b></td><td>Disaster recovery OpenShift cluster URL.</td></tr>
</table>

<h2>🔄 Pipeline Stages & Flow</h2>

<h3>1. Validate Allowed Environment per Branch</h3>
<p>This stage validates that the selected <code>AMBIENTE</code> is permitted for the current Git branch:</p>
<ul>
  <li>Branch <code>dev</code> allows environments <code>dev, uat</code>.</li>
  <li>Branch <code>main</code> allows all environments: <code>dev, uat, prd, drs</code>.</li>
</ul>
<p>If the environment is not permitted, an error is thrown.</p>

<h3>2. Checkout Project</h3>
<p>Clones the repository containing <code>ProjectsJenkinsCardifCSV.csv</code>.</p>

<h3>3. Read & Parse CSV Configuration</h3>
<p>Reads the CSV, finds the row matching <code>NOMBRE</code>, and writes it to <code>fila_<nombre>.json</code>.</p>

<h3>4. Country Confirmation</h3>
<p>Validates that the microservice’s <code>country</code> (from CSV) matches the parent folder in the Jenkins workspace path. Fails if mismatched.</p>

<h3>5. Generate <code>datos.txt</code> (if needed)</h3>
<p>If <code>RESOURCE_TYPE</code> is <code>configmap</code> or <code>secret</code>, this stage writes the <code>KEY_VALUE_PAIRS</code> parameter into <code>datos.txt</code>. Fails if no key‑value content provided.</p>

<h3>6. Generate Resource YAML</h3>
<p>Uses <code>JenkinsUtils</code> to compute the proper <code>oc</code> command to generate the resource YAML in dry‑run mode into <code>.yaml</code> file, e.g. <code>configmap-<nombre>.yaml</code> or <code>certificados-<nombre>-file.yaml</code>. Uses `MaskPasswordsBuildWrapper` for secure token masking.</p>

<h3>7. Backup</h3>
<p>Logs into OpenShift and runs a backup command with <code>oc get … -o yaml</code> for the resource into a timestamped YAML file inside a structured backup directory path. Adjusts file permissions with <code>chmod 644</code>.</p>

<h3>8. Delete Existing Resource (if exists)</h3>
<p>Checks if the resource exists. If it does, it deletes it. If not, logs “not found” and continues.</p>

<h3>9. Recreate Resource & Restart Deployment</h3>
<p>This final stage:</p>
<ul>
  <li>Applies the resource YAML.</li>
  <li>Extracts the content to the deployment environment (`oc extract`).</li>
  <li>Scales the deployment to zero replicas.</li>
  <li>If the resource is not <code>certificados</code>, cleans and sets environment variables from the resource.</li>
  <li>Scales the deployment back to 1 replica.</li>
</ul>
<p>All operations are performed inside a masked credentials wrapper and proper login/logout sequencing.</p>

<hr/>

<h2>⚙️ Resource Types & Commands</h2>
<p>The pipeline supports three resource types via a dynamic map in <code>JenkinsUtils.getResourceTypeMap()</code>:</p>
<ul>
  <li><code>configmap</code>: create, backup, extract, delete, apply configmap from <code>datos.txt</code>.</li>
  <li><code>secret</code>: same operations from <code>datos.txt</code>.</li>
  <li><code>certificados</code>: unstashes <code>FileCer.zip</code>, extracts cert files, and creates a secret containing files; also supports apply/backup/delete/extract.</li>
</ul>

<hr/>

<h2>📁 CSV Configuration Structure</h2>
<p>The pipeline expects <code>ProjectsJenkinsCardifCSV.csv</code> with semicolon-separated columns, including:</p>
<pre>
appName;country;usage;NameSpaceDev;TokenDev;NameSpaceUat;TokenUat;NameSpacePrd;TokenPrd;project
</pre>
<p>Each row should contain environment namespace and token, and optionally project name for backup path structuring.</p>

<hr/>

<h2>🧠 Important Notes & Best Practices</h2>
<ul>
  <li>Always include the microservice in the CSV ﬁle.</li>
  <li>Clean environment variables are removed and re‑set only for <code>configmap</code> and <code>secret</code>.</li>
  <li>The pipeline gracefully handles absence of existing resource.</li>
  <li>Strict login/logout sequence ensures tokens are masked and not leaked.</li>
  <li>Backup directory structure is: <code>/tmp/backup/${country}/${project}/${microservice}/${AMBIENTE}/RESOURCE_TYPE</code>.</li>
</ul>

<hr/>

<h2>📎 Related Resources</h2>
<ul>
  <li><a href="https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/developer-cli-commands.html">OpenShift CLI documentation</a></li>
  <li><a href="https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/">Kubernetes ConfigMap guide</a></li>
  <li><a href="https://www.jenkins.io/doc/book/security/">Jenkins security documentation</a></li>
</ul>
