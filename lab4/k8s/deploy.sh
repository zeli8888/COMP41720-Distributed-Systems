#!/bin/bash

kubectl apply -f namespace.yaml

kubectl apply -f persistent-volumes.yaml

kubectl apply -f configmaps.yaml
kubectl apply -f secrets.yaml

kubectl apply -f databases.yaml

echo "Waiting for database pods to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n microservices-lab --timeout=120s
kubectl wait --for=condition=ready pod -l app=mongodb -n microservices-lab --timeout=120s

kubectl apply -f kafka-ecosystem.yaml

echo "Waiting for kafka pods to be ready..."
kubectl wait --for=condition=ready pod -l app=broker -n microservices-lab --timeout=120s
kubectl wait --for=condition=ready pod -l app=zookeeper -n microservices-lab --timeout=120s

kubectl apply -f microservices.yaml

echo "deploy successfully！"
echo "access services:"
echo "API Gateway: http://$(minikube ip):30000"
echo "Kafka UI: http://$(minikube ip):30086"