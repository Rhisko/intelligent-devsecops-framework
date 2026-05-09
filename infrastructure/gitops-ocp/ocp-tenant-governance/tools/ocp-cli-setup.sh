#destroy crc VM if it is running
virsh -c qemu:///system destroy crc

#login to crc
oc login -u kubeadmin \
  -p '5ypcK-LQg32-pwqJ6-3J7R3' \
  https://api.crc.testing:6443 \
  --insecure-skip-tls-verify=true

#tunnel 

ssh -N -L 6443:api.crc.testing:6443 risko@34.45.31.227