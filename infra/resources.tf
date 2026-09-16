locals {
  zone = var.zone != "" ? var.zone : "${var.region}-a"
}

resource "google_compute_instance" "fly_mcp" {
  name         = "fly-mcp"
  machine_type = var.machine_type
  zone         = local.zone

  tags = ["fly-mcp"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = var.boot_disk_gb
      type  = "pd-balanced"
    }
  }

  network_interface {
    network = "default"
    # Ephemeral external IP: outbound for image pulls and dataset download,
    # inbound only on the firewalled MCP port.
    access_config {}
  }

  scheduling {
    automatic_restart   = true
    on_host_maintenance = "MIGRATE"
  }

  metadata = {
    fly-mcp-repo-url  = var.repo_url
    fly-mcp-repo-ref  = var.repo_ref
    fly-mcp-fly-brain = var.fly_brain
  }

  metadata_startup_script = file("${path.module}/startup.sh")

  service_account {
    # Default SA with minimal scope; the stack calls no Google APIs.
    email  = data.google_compute_default_service_account.default.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }
}

# The only inbound rule this stack creates. Everything else inbound comes from
# the project defaults (SSH, ICMP); port 8000 stays inside the Docker network.
resource "google_compute_firewall" "allow_mcp" {
  name    = "fly-mcp-allow-mcp"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = [var.app_port]
  }

  source_ranges = [var.allowed_source_cidr]
  target_tags   = ["fly-mcp"]
}

data "google_compute_default_service_account" "default" {}
