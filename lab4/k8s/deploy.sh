##!/bin/bash
#
#wait_with_timeout() {
#    local selector=$1
#    local name=$2
#    kubectl wait --for=condition=ready pod -l "$selector" -n microservices-lab --timeout=360s || echo "Warning: $name timeout, check with: kubectl get pods -l $selector -n microservices-lab"
#}
#
#wait_for_pvc() {
#    echo "Waiting for PersistentVolumeClaims to be bound..."
#    kubectl wait --for=jsonpath='{.status.phase}'=Bound pvc -n microservices-lab --timeout=120s || echo "Warning: Some PVCs not bound, check with: kubectl get pvc -n microservices-lab"
#}
#
#wait_a_moment() {
#    echo "Waiting for resources to initialize..."
#    sleep 5
#}
#
#echo "Step 1: Creating namespace and configurations..."
#kubectl apply -f namespace.yaml
#kubectl apply -f configmaps.yaml
#kubectl apply -f secrets.yaml
#wait_a_moment
#
#echo "Step 2: Setting up storage..."
#kubectl apply -f persistent-volumes.yaml
#wait_for_pvc
#
#echo "Step 3: Deploying databases..."
#kubectl apply -f databases.yaml
#echo "Waiting for database pods to be ready..."
#wait_with_timeout "app=postgres" "PostgreSQL"
#wait_with_timeout "app=mongodb" "MongoDB"
#
#echo "Step 4: Deploying Kafka ecosystem..."
#kubectl apply -f kafka-ecosystem.yaml
#echo "Waiting for kafka pods to be ready..."
#wait_with_timeout "app=broker" "Kafka Broker"
#wait_with_timeout "app=zookeeper" "Zookeeper"
#wait_with_timeout "app=schema-registry" "Schema Registry"
#wait_with_timeout "app=kafka-ui" "Kafka UI"
#
#echo "Step 5: Deploying microservices..."
#kubectl apply -f microservices.yaml
#echo "Waiting for microservices pods to be ready..."
#wait_with_timeout "app=order-service" "Order Service"
#wait_with_timeout "app=inventory-service" "Inventory Service"
#wait_with_timeout "app=notification-service" "Notification Service"
#wait_with_timeout "app=api-gateway" "API Gateway"
#
#echo "Deploy completed!"
#echo "Access services:"
#echo "API Gateway: minikube service api-gateway -n microservices-lab"
#echo "Kafka UI: minikube service kafka-ui -n microservices-lab"
#
#echo ""
#echo "To check all pods: kubectl get pods -n microservices-lab"
#echo "To check detailed events: kubectl get events --sort-by='.lastTimestamp' -n microservices-lab"