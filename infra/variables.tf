variable "project_id" {
  description = "GCP project id with billing enabled"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "europe-west4"
}

variable "zone" {
  description = "GCP zone (empty means region plus -a)"
  type        = string
  default     = ""
}

variable "machine_type" {
  description = "Machine type. e2-highmem-4 = 4 vCPU / 32GB, enough for the MaleCNS kernel plus the MCP server."
  type        = string
  default     = "e2-highmem-4"
}

variable "boot_disk_gb" {
  description = "Boot disk size in GB (MaleCNS dataset and the Docker images need room)"
  type        = number
  default     = 25
}

variable "repo_url" {
  description = "Public repo the VM clones on boot"
  type        = string
  default     = "https://github.com/franBec/fly-mcp.git"
}

variable "repo_ref" {
  description = "Git ref the VM checks out (branch or commit sha)"
  type        = string
  default     = "main"
}

variable "fly_brain" {
  description = "Brain implementation on the VM: real (MaleCNS dataset) or mock"
  type        = string
  default     = "real"
}

variable "app_port" {
  description = "Published MCP server port"
  type        = number
  default     = 8080
}

variable "allowed_source_cidr" {
  description = "The one CIDR allowed to reach the MCP port. Pass your own /32: -var=\"allowed_source_cidr=$(curl -4 -s ifconfig.me)/32\""
  type        = string
}
