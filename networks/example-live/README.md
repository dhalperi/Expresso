# example-live

This network is Batfish's `live` example snapshot, used here as a small,
runnable end-to-end sample for Expresso.

`raw-configs/` is copied verbatim from open-source Batfish, from
`networks/example/live/configs` at commit
[`fac6dfc91b`](https://github.com/batfish/batfish/tree/fac6dfc91b/networks/example/live/configs).
It is a multi-AS BGP topology (AS 1/2/3) that exercises route-maps, community
lists, and AS-path matching. These configs are Apache-2.0 licensed, like Batfish
itself.

Only `raw-configs/` is checked in. Generate the vendor-independent model
(`configs/`) and inferred `topology.txt` on demand with the Batfish parser:

```bash
java -cp "target/batfish-parser.jar:lib/batfish/batfish-thin.jar" \
  util.BatfishUtil example-live
```

Then run Expresso on it as described in the top-level [README](../../README.md).
