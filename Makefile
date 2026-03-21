.PHONY: generate clean

PROTO_DIR := proto
GO_OUT := go

generate:
	find $(PROTO_DIR) -name '*.proto' | xargs protoc \
		--proto_path=$(PROTO_DIR) \
		--go_out=$(GO_OUT) --go_opt=paths=source_relative \
		--go-grpc_out=$(GO_OUT) --go-grpc_opt=paths=source_relative

clean:
	find $(GO_OUT) -name '*.pb.go' -delete
