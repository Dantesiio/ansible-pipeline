# ansible-pipeline

Playbook para aprovisionar la infraestructura de CI/CD del taller sobre una sola VM:

- **Host `jenkins`**: instala Docker Engine, levanta un stack compuesto por Jenkins LTS (JDK17), SonarQube 9.9 LTS, PostgreSQL 15 y un contenedor Nginx que sirve la aplicación estática `Teclado`. Configura Jenkins con plugins críticos, crea el usuario administrador y registra la instancia Sonar.

## Requisitos previos

1. Python 3.10+ con `pip` disponible en el equipo de control.
2. Crear un entorno virtual aislado (recomendado para evitar el error de entorno administrado en macOS):

```bash
python3 -m venv .venv
source .venv/bin/activate
```

3. Dependencias Python para Ansible:

```bash
pip install -r requirements.txt
```

4. Colecciones de Ansible necesarias:

```bash
ansible-galaxy collection install -r collections/requirements.yml
```

5. `rsync` instalado tanto en la máquina de control como en los hosts gestionados (el playbook lo instala automáticamente en las VMs Ubuntu).

## Variables relevantes

El playbook lee las siguientes variables de entorno (todas opcionales, pero recomendadas):

- `JENKINS_ADMIN_ID`: usuario administrador inicial de Jenkins (`admin` por defecto).
- `JENKINS_ADMIN_PASSWORD`: contraseña del administrador (`ChangeMe123!` por defecto).
- `SONAR_ADMIN_TOKEN`: token PAT creado en SonarQube para registrar la credencial `sonarqube-admin-token` en Jenkins.

Puedes sobreescribir cualquier valor en tiempo de ejecución usando `-e VAR=valor` o creando un archivo de inventario con variables de host/grupo.

## Ejecución

1. (Opcional) Activa el entorno virtual creado anteriormente:

```bash
source .venv/bin/activate
```

2. Exporta (o define) las variables de entorno deseadas.
3. Ejecuta el playbook indicando el inventario con la IP pública generada por Terraform:

```bash
.venv/bin/ansible-playbook -i inventory.ini playbook.yml
```

El inventario mínimo contiene únicamente el grupo `jenkins`:

```ini
[jenkins]
jenkins ansible_host=XX.XX.XX.XX ansible_user=adminuser ansible_ssh_private_key_file=/ruta/completa/.ssh/taller_devops ansible_ssh_common_args='-o StrictHostKeyChecking=no'
```

> **Nota:** asegúrate de ejecutar el playbook desde la carpeta `ansible-pipeline`, ya que las rutas relativas (por ejemplo `../Teclado`) dependen de esa estructura para sincronizar la aplicación.

## Resultado esperado

- Jenkins disponible en `http://<IP_JENKINS>:8080` con el usuario y contraseña definidos.
- SonarQube disponible en `http://<IP_JENKINS>:9000` usando la base de datos PostgreSQL interna.
- Servicio Nginx sirviendo la aplicación `Teclado` en `http://<IP_JENKINS>` (puerto 80).
- Jenkins preconfigurado con plugins: Configuration as Code, SonarQube, Pipeline, Blue Ocean, Git, Docker Workflow, entre otros.
- Credencial `sonarqube-admin-token` creada automáticamente cuando se define `SONAR_ADMIN_TOKEN`.
