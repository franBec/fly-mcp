output "instance_ip" {
  description = "Ephemeral public IP of the VM"
  value       = google_compute_instance.fly_mcp.network_interface[0].access_config[0].nat_ip
}

output "mcp_url" {
  description = "Remote MCP endpoint"
  value       = "http://${google_compute_instance.fly_mcp.network_interface[0].access_config[0].nat_ip}:${var.app_port}/mcp"
}

output "opencode_add_command" {
  description = "Register the fly in opencode"
  value       = "opencode mcp add fly-mcp --url http://${google_compute_instance.fly_mcp.network_interface[0].access_config[0].nat_ip}:${var.app_port}/mcp"
}

output "ssh_command" {
  description = "SSH into the VM"
  value       = "gcloud compute ssh fly-mcp --zone=${local.zone}"
}

output "logs_command" {
  description = "Watch the stack boot"
  value       = "gcloud compute ssh fly-mcp --zone=${local.zone} --command='sudo docker compose -f /opt/fly-mcp/compose.yml ps && sudo docker compose -f /opt/fly-mcp/compose.yml logs -f warmup mcp'"
}
