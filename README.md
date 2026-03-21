# nacos-sdk-proto

Shared Protocol Buffers definitions for Nacos multi-language SDKs.

This repository provides a single source of truth for all gRPC message types used in Nacos client-server communication. Multi-language SDKs (Go, Rust, Python, etc.) can generate native code from these proto files instead of manually maintaining JSON serialization logic.

**Proposal**: [alibaba/nacos#14683](https://github.com/alibaba/nacos/issues/14683)

## Repository Structure

```
nacos-sdk-proto/
├── proto/                          # Proto definitions (source of truth)
│   ├── nacos_grpc_service.proto    # Transport layer: Payload, Metadata, gRPC services
│   ├── common/
│   │   └── common.proto            # Connection lifecycle messages (9 types)
│   ├── config/
│   │   ├── config_request.proto    # Config requests (5 types)
│   │   └── config_response.proto   # Config responses (4 types)
│   └── naming/
│       ├── instance.proto          # Instance and ServiceInfo domain objects
│       ├── naming_request.proto    # Naming requests (3 types)
│       └── naming_response.proto   # Naming responses (3 types)
├── go/                             # Generated Go code
├── docs/
│   └── type-registry.json          # Registry of all 24 metadata.type values
├── buf.yaml                        # Buf linter config
├── Makefile                        # Build targets
├── go.mod
└── go.sum
```

## Message Types

There are 24 message types used as `metadata.type` values in the Nacos gRPC protocol:

| Module | Types | Description |
|--------|-------|-------------|
| common | 9 | Connection setup, health check, server check, error |
| config | 9 | Config CRUD, batch listen, change notification |
| naming | 6 | Instance register/deregister, service query, subscribe |

See [`docs/type-registry.json`](docs/type-registry.json) for the complete registry with direction and version info.

## Code Generation

### Go

```bash
make generate
```

Requires `protoc`, `protoc-gen-go`, and `protoc-gen-go-grpc`:

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
```

### Rust

```bash
protoc --proto_path=proto \
  --prost_out=rust/src \
  proto/**/*.proto proto/nacos_grpc_service.proto
```

Or use [tonic-build](https://docs.rs/tonic-build) in `build.rs`.

### Python

```bash
python -m grpc_tools.protoc \
  --proto_path=proto \
  --python_out=python \
  --grpc_python_out=python \
  proto/**/*.proto proto/nacos_grpc_service.proto
```

### Other Languages

Use `protoc` with the appropriate language plugin. All proto files are under the `proto/` directory.

## Proto Naming Conventions

### Field Names

Field names use **camelCase** to match Java field names exactly. This is intentional — the Nacos server uses Protobuf JSON (protojson) serialization where field names map directly to JSON keys. Using camelCase in proto ensures the generated code produces JSON that the server understands without custom field mappings.

Example: `requestId`, `dataId`, `clientVersion`, `abilityTable`

> Note: This deviates from the proto3 convention of `snake_case` field names. The `buf.yaml` config excludes `FIELD_LOWER_SNAKE_CASE` lint rule for this reason.

### Flattened Inheritance

Java Nacos uses class inheritance (e.g., `ConfigQueryRequest extends ConfigRequest extends Request`). Proto3 does not support inheritance, so all fields from the class hierarchy are flattened into a single message. Common fields like `requestId`, `resultCode`, `errorCode`, and `message` are repeated in each message where they appear.

## Wire Format

The Nacos gRPC protocol wraps all business messages in a `Payload` envelope:

```
Payload {
  metadata: Metadata {
    type: "ConfigQueryRequest"    // Java SimpleName, used for deserialization routing
    clientIp: "10.0.0.1"
    headers: { ... }
  }
  body: Any {                     // google.protobuf.Any containing protojson-encoded bytes
    value: <JSON bytes>
  }
}
```

The `body.value` is **not** standard protobuf binary encoding — it is JSON bytes serialized with protojson. This allows the server to deserialize using its existing Jackson-based JSON infrastructure while SDKs can use protojson for type-safe serialization.

## License

Apache License 2.0
