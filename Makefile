.PHONY: generate generate-proto generate-go generate-python generate-nodejs \
        clean verify sync-go-mod migrate

# === 仓库配置（转移时只改这一行） ===
REPO_OWNER     := cxhello
REPO_NAME      := nacos-sdk-proto
GO_MODULE_BASE := github.com/$(REPO_OWNER)/$(REPO_NAME)/go

# === 路径 ===
PROTO_DIR      := proto
GO_OUT         := go
PYTHON_OUT     := python
NODEJS_OUT     := nodejs
GENERATOR_DIR  := tools/proto-generator
LOCK_FILE      := $(GENERATOR_DIR)/field-numbers.json

# === Proto 生成（Java 反射 → .proto） ===
generate-proto:
	cd $(GENERATOR_DIR) && mvn -q compile exec:java \
		-Dexec.mainClass="com.alibaba.nacos.proto.generator.ProtoGenerator" \
		-Dexec.args="--output ../../$(PROTO_DIR) --lockfile ../../$(LOCK_FILE) \
		  --go-module-base $(GO_MODULE_BASE)"

# === 各语言代码生成 ===
generate: generate-go

generate-go:
	find $(PROTO_DIR) -name '*.proto' | xargs protoc \
		--proto_path=$(PROTO_DIR) \
		--go_out=$(GO_OUT) --go_opt=paths=source_relative \
		--go-grpc_out=$(GO_OUT) --go-grpc_opt=paths=source_relative

generate-python:
	python -m grpc_tools.protoc \
		--proto_path=$(PROTO_DIR) \
		--python_out=$(PYTHON_OUT)/nacos_sdk_proto \
		--grpc_python_out=$(PYTHON_OUT)/nacos_sdk_proto \
		$$(find $(PROTO_DIR) -name '*.proto')

generate-nodejs:
	find $(PROTO_DIR) -name '*.proto' | xargs protoc \
		--proto_path=$(PROTO_DIR) \
		--plugin=protoc-gen-ts=./node_modules/.bin/protoc-gen-ts \
		--ts_out=$(NODEJS_OUT)/src \
		--grpc_out=$(NODEJS_OUT)/src

clean:
	find $(GO_OUT) -name '*.pb.go' -delete

sync-go-mod:
	cd $(GO_OUT) && go mod edit -module $(GO_MODULE_BASE)

verify: generate-proto generate
	cd $(GO_OUT) && go build ./...

migrate:
	$(MAKE) generate-proto
	$(MAKE) generate
	$(MAKE) sync-go-mod
	@echo "Done. Review changes and commit."
