# internet2

The Internet2 backbone, used as the public evaluation dataset in the Expresso paper.

The checked-in vendor-independent model (`configs/`) and inferred `topology.txt` are
what Expresso consumes directly, as in the original release.

`raw-configs/` are the raw Juniper configurations (10 routers: atla, chic, clev,
hous, kans, losa, newy32aoa, salt, seat, wash), originally from the
[bagpipe](https://github.com/konne88/bagpipe) project and lightly cleaned. They are
kept so the VI can be regenerated against a newer Batfish if its model changes:

```bash
java -cp "target/batfish-parser.jar:lib/batfish/batfish-thin.jar" \
  util.BatfishUtil internet2
```

`isisTopology.json` and `knownExternalRoutes.xml` are additional inputs consumed by
Expresso (IS-IS adjacencies and a snapshot of externally-learned routes).

Then run Expresso as described in the top-level [README](../../README.md), e.g.:

```bash
java -Xmx32g \
  -cp "target/expresso.jar:lib/batfish/batfish-thin.jar:lib/jdd-111.jar" \
  application.experiments.ExpressoRunner comm asp internet2
```
