# Expresso: Comprehensively Reasoning About External Routes Using Symbolic Simulation

This project is a prototype of the ACM SIGCOMM 2024 paper "Expresso: Comprehensively Reasoning About External Routes Using Symbolic Simulation" by Dan Wang, Peng Zhang, Aaron Gember-Jacobson.

## Abstract

Existing network verifiers can efficiently identify failure-induced bugs.
However, an equally-important concern is identification of external-routes-induced bugs, which has not been well addressed.
Comprehensively reasoning about external routes is challenging, since each external neighbor can advertise an arbitrary set of routes, which is quite a huge space.
This paper introduces a new network verifier, Expresso, which uses symbolic simulation to explore the equivalences in the space of external routes.
We evaluate the effectiveness and scalability of Expresso on the WAN of a large cloud service provider and Internet2.
Expresso found various property violations, some of which have already been confirmed by the operators.
To the best of our knowledge, Expresso is the only verifier that can check the correctness of WANs amidst arbitrary external routes in a tractable amount of time, while other verifiers time-out after 1 day.

## Build and Run Expresso

### Build Expresso

Execute the following command under the project root folder.

```bash
mvn package
```

### Organize inputs

Please make a new `<net-name>` folder under the `networks` folder.

```text
networks
    |---<net-name>
            |---raw-configs
            |---configs
            |---topology.txt
```

You can either provide only `raw-configs`, or provide `configs` + `topology.txt`:
- `raw-configs`: raw vendor (e.g., cisco) configuration files.
- `configs`: vendor independent configuration files (JSON format).
- `topology.txt`: interface level topology.

But we highly recommend you to provide `configs` + `topology.txt`, see [input parser](#input-parser).

### Run Expresso

```bash
# if you are providing raw vendor-specific configurations, 
# call the batfish parser to convert them into vendor-independent configurations
java -jar target/batfish-parser.jar <name>

# appoint "comm" if to consider symbolic communities
# appoint "asp" if to consider symbolic as paths
# appoint "tp" if to consider traffic policies
java -jar target/expresso.jar comm asp tp <name>
```

Note that when running Expresso, we highly recommend you to permit a high memory usage for JVM by appointing -Xmx.

## Project Structure

- `lib`: library jar files, including JDD and some Batfish modules.
- `networks`: input datasets and examples.
- `src/inputparser`: parsers that convert JSON configuration files into Expresso's network model.
- `src/controlplane`: control-plane models and symbolic simulation engine, including `Router`, `BgpRoutingProcess`, and `CPExecutor`.
- `src/dataplane`: symbolic data-plane analysis engine, including `DPAnalyzer`.
- `src/propertychecker`: routing and forwarding property checkers, such as RouteLeakFree, RouteHijackFree, and Reachability.
- `src/application`: main entry points and evaluation applications, such as `ExpressoRunner`.
- `src/atomic`: atomic predicate computers based on automata or BDDs, inspired by Batfish `SearchRoutePolicies`.
- `src/datamodel`: common networking models, such as `Ip` and `RoutePolicy`.
- `src/bdd`: wrappers for common BDD operations.
- `src/algorithm`: boolean expression minimization and graph algorithms.
- `src/main`: controller, logger, configuration, and storage.
- `src/util`: utility classes, such as `JsonUtil` and `MathUtil`.

## Notes

### Dataset

In our paper, we use two WAN configuration snapshots of a large cloud service provider (CSP) and the Internet2 configurations to evaluate Expresso.
Due to privacy constraints, we do not release the CSP WAN configurations in this repository.
We include the Internet2 configurations in [JSON format parsed by Batfish](networks/internet2).

### Input Parser

Our input parser (i.e., `src/inputparser`) converts JSON format configurations into the network models used by Expresso.
The JSON config we are parsing ([examples here](networks/example)) is largely the same with the Batfish VI (Vendor-independent configuration), but with some simplifications.

We are not using the Batfish VI directly mainly because its modeling of routing policy is different from Expresso's modeling.
Batfish converts vendor-specific routing policy configured as plain match-action clauses into a common vendor-independent policy AST, while Expresso models match-action clauses.
Batfish also combines raw peer export policy together with other routing semantics, such as route origination, redistribution, and aggregation, into a peer-specific wrapper policy to handle them together.
However, Expresso processes these pieces separately.

Currently, we only partially support parsing Batfish VI, so Expresso may produce `UnsupportedOperationException` during parsing if you provide raw vendor-specific configs.
We therefore highly recommend users provide JSON format configurations directly.

## Bibtex

```text
@inproceedings{wang2024expresso,
  author = {Wang, Dan and Zhang, Peng and Gember-Jacobson, Aaron},
  title = {Expresso: Comprehensively Reasoning About External Routes Using Symbolic Simulation},
  year = {2024},
  isbn = {9798400706141},
  publisher = {Association for Computing Machinery},
  address = {New York, NY, USA},
  url = {https://doi.org/10.1145/3651890.3672220},
  doi = {10.1145/3651890.3672220},
  abstract = {Existing network verifiers can efficiently identify failure-induced bugs. However, an equally-important concern is identification of external-routes-induced bugs, which has not been well addressed. Comprehensively reasoning about external routes is challenging, since each external neighbor can advertise an arbitrary set of routes, which is quite a huge space. This paper introduces a new network verifier, Expresso, which uses symbolic simulation to explore the equivalences in the space of external routes. We evaluate the effectiveness and scalability of Expresso on the WAN of a large cloud service provider and Internet2. Expresso found various property violations, some of which have already been confirmed by the operators. To the best of our knowledge, Expresso is the only verifier that can check the correctness of WANs amidst arbitrary external routes in a tractable amount of time, while other verifiers time-out after 1 day.},
  booktitle = {Proceedings of the ACM SIGCOMM 2024 Conference},
  pages = {197–212},
  numpages = {16},
  keywords = {network verification, external route, equivalence classes, symbolic simulation},
  location = {Sydney, NSW, Australia},
  series = {ACM SIGCOMM '24}
}
```

## Contact

- Dan Wang (dan-wang@stu.xjtu.edu.cn or danw853211@gmail.com)
- Peng Zhang (p-zhang@xjtu.edu.cn)

## License

Apache-2.0 License, see [LICENSE](LICENSE.txt).
