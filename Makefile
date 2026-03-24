.PHONY: generate generate-proto generate-go generate-python generate-nodejs \
        clean verify sync-go-mod migrate sync setup update-version

# === 仓库配置（转移时只改这一行） ===
REPO_OWNER     := cxhello
REPO_NAME      := nacos-sdk-proto
GO_MODULE_BASE := github.com/$(REPO_OWNER)/$(REPO_NAME)/go

# === Nacos 源码 ===
NACOS_REPO     := https://github.com/alibaba/nacos.git
NACOS_DIR      := .nacos

# === 动态版本（从 .nacos/pom.xml 读取） ===
NACOS_VERSION  = $(shell grep -m1 '<revision>' $(NACOS_DIR)/pom.xml 2>/dev/null | sed 's/.*<revision>\(.*\)<\/revision>.*/\1/')

# === 路径 ===
PROTO_DIR      := proto
GO_OUT         := go
PYTHON_OUT     := python
NODEJS_OUT     := nodejs
GENERATOR_DIR  := tools/proto-generator
LOCK_FILE      := $(GENERATOR_DIR)/field-numbers.json

# === 一键同步（本地入口） ===
sync:
	@./scripts/sync.sh

# === 拉取 Nacos 源码 + 构建 nacos-api ===
setup:
	@if [ -d $(NACOS_DIR) ]; then \
		echo "Fetching nacos develop HEAD..."; \
		git -C $(NACOS_DIR) fetch origin develop --depth=1 -q; \
		git -C $(NACOS_DIR) reset --hard FETCH_HEAD -q; \
	else \
		echo "Cloning nacos..."; \
		git clone --depth=1 --branch develop $(NACOS_REPO) $(NACOS_DIR); \
	fi
	cd $(NACOS_DIR) && mvn install -pl api -am -DskipTests -Drat.skip=true -q

# === Proto 生成（Java 反射 → .proto） ===
generate-proto:
	cd $(GENERATOR_DIR) && mvn -q compile exec:java \
		$(if $(NACOS_VERSION),-Dnacos.version=$(NACOS_VERSION),) \
		-Dexec.mainClass="com.alibaba.nacos.proto.generator.ProtoGenerator" \
		-Dexec.args="--output ../../$(PROTO_DIR) --lockfile ../../$(LOCK_FILE) \
		  --go-module-base $(GO_MODULE_BASE)"

# === 各语言代码生成 ===
generate: generate-go generate-nodejs

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
	find $(PROTO_DIR) -name '*.proto' -not -name 'nacos_grpc_service.proto' | xargs protoc \
		--plugin=./node_modules/.bin/protoc-gen-ts_proto \
		--ts_proto_out=$(NODEJS_OUT)/src \
		--ts_proto_opt=outputJsonMethods=true,outputEncodeMethods=false,outputClientImpl=false \
		--proto_path=$(PROTO_DIR)

clean:
	find $(GO_OUT) -name '*.pb.go' -delete

sync-go-mod:
	cd $(GO_OUT) && go mod edit -module $(GO_MODULE_BASE)

# === 更新 VERSION 溯源文件 ===
update-version:
	@SHA=$$(git -C $(NACOS_DIR) rev-parse HEAD); \
	DATE=$$(date -u +%Y-%m-%dT%H:%M:%SZ); \
	printf '{\n  "source": "local",\n  "nacos_ref": "develop",\n  "nacos_commit": "%s",\n  "generated_at": "%s"\n}\n' \
		"$$SHA" "$$DATE" > $(PROTO_DIR)/VERSION

verify:
	cd $(GO_OUT) && go build ./...

migrate:
	$(MAKE) generate-proto
	$(MAKE) generate
	$(MAKE) sync-go-mod
	sed -i '' 's|github.com/[^/]*/$(REPO_NAME)|github.com/$(REPO_OWNER)/$(REPO_NAME)|g' \
		$(NODEJS_OUT)/package.json $(PYTHON_OUT)/pyproject.toml \
		README.md README_zh.md
	@echo "Done. Review changes and commit."
