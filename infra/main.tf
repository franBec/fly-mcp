terraform {
  required_version = ">= 1.6.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
  # Disposable POC: local state is deliberate. terraform destroy removes the VM,
  # the firewall rule, and the state that remembers them.
}

provider "google" {
  project = var.project_id
  region  = var.region
}
