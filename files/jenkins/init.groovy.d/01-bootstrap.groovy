import jenkins.model.Jenkins
import hudson.security.*
import hudson.util.Secret
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.StandardCredentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig

def instance = Jenkins.get()
def env = System.getenv()

def adminId = env.getOrDefault('JENKINS_ADMIN_ID', 'admin')
def adminPassword = env.getOrDefault('JENKINS_ADMIN_PASSWORD', 'ChangeMe123!')

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(adminId, adminPassword)
instance.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

instance.save()

def sonarToken = env.get('SONAR_ADMIN_TOKEN')
if (sonarToken != null && sonarToken.trim()) {
  def credentialsStore = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
  def domain = Domain.global()

  def existing = CredentialsProvider.lookupCredentials(
      StandardCredentials.class,
      instance,
      null,
      [] as List
  ).find { it.id == 'sonarqube-admin-token' }

  if (existing == null) {
    def creds = new StringCredentialsImpl(
      CredentialsScope.GLOBAL,
      'sonarqube-admin-token',
      'Token administrado para SonarQube',
      Secret.fromString(sonarToken)
    )
    credentialsStore.addCredentials(domain, creds)
  }

  try {
    def sonarConfig = instance.getDescriptorByType(SonarGlobalConfiguration)
    def installations = sonarConfig.getInstallations() as List
    def existingInstallation = installations.find { it.name == 'sonarqube' }
    if (existingInstallation == null) {
      def newInstallation = new SonarInstallation(
        'sonarqube',
        env.getOrDefault('SONARQUBE_URL', 'http://sonarqube:9000'),
        '',
        '',
        'sonarqube-admin-token',
        null,
        null,
        null,
        false,
        false,
        new TriggersConfig(),
        ''
      )
      installations.add(newInstallation)
      sonarConfig.setInstallations(installations)
    }
    sonarConfig.save()
  } catch (Throwable sonarError) {
    println("[bootstrap] SonarQube plugin not initialized yet: ${sonarError.message}")
  }
}
