.PHONY: generate clean generate-proto generate-proto-check verify-proto

PROTO_DIR := proto
GO_OUT := go
GENERATOR_DIR := tools/proto-generator
LOCK_FILE := field-numbers.json

generate:
	find $(PROTO_DIR) -name '*.proto' | xargs protoc \
		--proto_path=$(PROTO_DIR) \
		--go_out=$(GO_OUT) --go_opt=paths=source_relative \
		--go-grpc_out=$(GO_OUT) --go-grpc_opt=paths=source_relative

clean:
	find $(GO_OUT) -name '*.pb.go' -delete

generate-proto:
	cd $(GENERATOR_DIR) && mvn -q compile exec:java \
		-Dexec.mainClass="com.alibaba.nacos.proto.generator.ProtoGenerator" \
		-Dexec.args="--output ../../$(PROTO_DIR) --lockfile $(LOCK_FILE)"

generate-proto-check:
	cd $(GENERATOR_DIR) && mvn -q compile exec:java \
		-Dexec.mainClass="com.alibaba.nacos.proto.generator.ProtoGenerator" \
		-Dexec.args="--output ../../$(PROTO_DIR) --lockfile $(LOCK_FILE) --dry-run"

verify-proto: generate-proto generate
	go build ./go/...
