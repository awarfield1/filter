# Filter API

A small, dependency-free Java library for filtering resources.

## Filter Model

A `resource` is a `Map<String, String>` of property names to values.

    {
      "firstname" -> "Joe",
      "role" -> "administrator",
      "age" -> "35"
    }

A `filter` is an immutable, composable predicate over resources.
```java
    boolean matches(Map<String, String> resource);
```


- A filter forms a tree.
- The leaves are the actual predicates.
- The branches are logical operators.
- The branches combine predicates into complex expressions.

```text
                      Operator
                     /        \
                 Operator   Predicate
                /        \
          Predicate    Predicate
```

Every filter answers the same question: 

> Does this resource match?

## Core types

The root interface is `Filter`.

The core filter types implement `Filter` directly. They are our API's built-in vocabulary. They have dedicated visitors.

    BooleanLiteral
    And
    Or
    Not
    PropertyPresent
    PropertyComparison
    PropertyMatchesRegex

Core filter types are divided into two categories: leaf filters and composite filters.


## Leaf filters

These are the terminal nodes of the tree. They represent the actual predicates that are tested against a resource.

### BooleanLiteral

These are the simplest filters. They always return the same result.

    TRUE
    FALSE

Examples:

    (true)
    (false)

### PropertyPresent

These check for the presence of a property in a resource.

    (email=*)

means:

    resource has an "email" property with any value.

### PropertyComparison

These compare a property's value to some operand.

    (role=administrator)
    
    (age>30)
    
    (role!=guest)

### PropertyMatchesRegex

These compare a property's value to a regex.

Example:

    (surname~=^bl)


## Composite filters

These build trees.

### And

    (&(role=administrator)(age>30))

means:

    role == administrator
    AND
    age > 30

### Or

    (|(role=administrator)(role=auditor))

means:

    role == administrator
    OR
    role == auditor

### Not

    (!(role=administrator))

means:

    NOT (role == administrator)

Complex example:

    (&
        (|(role=administrator)(role=auditor))
        (age>30)
        (email=*)
        (!(status=disabled))
    )

means:

    (role == administrator OR role == auditor)
    AND age > 30
    AND email is present
    AND status is not disabled

Tree structure:

```text
                                AND
               /           |           |           \
             OR         age > 30    email=*       NOT
           /    \                                  |
role=administrator role=auditor            status=disabled
```

Instead of constructing filters directly, use the `Filters` factory class. This is our public API.

Example:

```Java
    Filter filter = Filters.and(
        Filters.or(
            Filters.equalTo("role", "administrator"),
            Filters.equalTo("role", "auditor")
        ),
    
        Filters.greaterThan("age", "30"),
    
        Filters.present("email"),
    
        Filters.not(
            Filters.equalTo("status", "disabled")
        )
    );
```

Matching resource:

    {
        "role"   -> "administrator",
        "age"    -> "35",
        "email"  -> "joe@example.com",
        "status" -> "active"
    }

This returns true because all the conditions are satisfied:

1. The role is `administrator` which matches the first `OR` condition.
2. The age is `35` which is `greater` than `30`.
3. The email property is `present`.
4. The status is `active` which is not equal to `disabled`.

Non-matching resource:

    {
        "role"   -> "auditor",
        "age"    -> "22",
        "email"  -> "joe@example.com",
        "status" -> "active"
    }

This returns false because the age condition is not satisfied. The age is `22` which is not `greater` than `30`.


## Structural Processing and the Visitor Pattern

Third-parties can do meaningful work with the structure of our filters.

Examples of work:

    Convert a filter to SQL
    Collect referenced fields
    Optimize a filter
    Estimate query cost
    Generate a description

The visitor pattern separates operations from the filter implementation. Rather than embedding every possible operation into the filter classes, a visitor traverses the tree and performs the work based on the node type it encounters.

### FilterVisitor

`FilterVisitor` is the interface for traversing the filter tree. Without this you would end up doing brittle casting and `instanceof` checks.

Without a visitor:
```Java
    if (filter instanceof And)
```

With a visitor:
```Java
    filter.accept(visitor, context);
```

The call to `accept()` performs dispatch. Each filter implementation routes the visitor to the correct `visitXxx(...)` method based on the filter's concrete type.

Example dispatch:
```Java
    visitAnd(...)
```

### Canonical Visitors

An API should provide canonical examples of visitors that do useful work. Two visitors are provided that should be useful for many applications. They are not required by the spec but they are good examples of how to use the visitor pattern.

### PropertyCollector

Walks the tree and finds every property used in a core filter type. It throws when visiting an extension filters because their semantics are unknown and may produce incomplete results. See the Semantic capabilites section for a future enhancement.

Input:

    (&
        (|(role=administrator)(role=auditor))
        (age>30)
        (email=*)
        (!(status=disabled))
    )

Output:

    ["role", "age", "email", "status"]

### FilterDescriber

Turns core filters into English expressions. Extension filters are described generically due to unknown semantics.

Input:

    (&
        (|(role=administrator)(role=auditor))
        (age>30)
        (email=*)
        (!(status=disabled))
    )

Output:

    (role is administrator or role is auditor)
    and age is greater than 30
    and email is present
    and not (status is disabled)

### Domain specific visitors

For third-parties with domain-specific requirements, they implement their own visitors.

### CostEstimator Example:
Imagine a visitor that estimates the cost of evaluating a filter. It could assign a cost to each node type and aggregate during tree traversal.
The caller does not need to know anything about the structure of the filter tree. They invoke the estimator and receive a result.

```Java
class CostEstimator implements FilterVisitor<Integer, Void>
```

Some visitor methods could be assigned static costs:

```Java
    @Override
    public Integer visitBooleanLiteral(BooleanLiteral filter, Void context) {
        return 1;
    }
```

Others may calculate the sum of their operands:

```Java
    @Override
    public Integer visitAnd(And filter, Void unused) {
        return filter.operands()
                .stream()
                .mapToInt(op -> op.accept(this, null))
                .sum();
    }
```

An entry-point method would call `accept()` to perform the dispatch and return the cost estimate:

```Java
    public int estimate(Filter filter) {
        return filter.accept(this, null);
    }
```

The API user would simply invoke the estimator:
```Java
int cost = new CostEstimator().estimate(filter);
```

This example demonstrates how a visitor can traverse the filter tree and produce arbitrary results without depending on the concrete filter implementations.

## Extending filter types

`ExtensibleFilter` is the mechanism for adding new filter types without modifying the core library.

It exposes:

    extensionId()
    parameters()
    operands()

Generic consumers can inspect an extension through its `identifier`, `parameters`, and `operands` without knowing the concrete implementation class.

### OneOf example

Imagine a third-party wants a `OneOf` filter type that matches if a property is equal to at least one of a list of values.

This is not a core filter type today. But could be added:

    record OneOf(...) implements ExtensibleFilter

Extension id:

    example.com:filter:oneOf

Parameters:

    property=role
    values=administrator,auditor

Semantics:

    role IN ("administrator", "auditor")

Code snippet:

```java
    @Override
        public boolean matches(Map<String, String> resource) {
        String actual = resource.get(property);
        return actual != null && values.stream().anyMatch(actual::equalsIgnoreCase);
    }
```

A generic visitor can still traverse, display or reject this filter type without knowing the `OneOf` class implementation.

This is an explicit compromise to allow third parties to add new filter types without having to modify the core library.
And is a tradeoff between type safety and extensibility. See spec 5a and 5b.

## Comparison semantics

Resources are represented as Map<String, String> so every property value is stored as a string.

This creates ambiguous comparisons.

Example:
```Java
Filters.greaterThan("age", "30");
```
Should "100" be compared numerically or alphabetically?

### MatchingRule

A `MatchingRule` defines how values for a property are compared.

Built-in rules:

    AUTO         -> Numeric when both values look numeric, otherwise case-insensitive text
    
    NUMERIC      -> Always compare as decimal numbers
    
    CASE_IGNORE  -> Always compare as case-insensitive text
    
    CASE_EXACT   -> Always compare as case-sensitive text

`AUTO` is the default rule.

It attempts numeric comparison when both values appear numeric:

    "100" > "35"

Otherwise it performs a case-insensitive text comparison:

    "ADMINISTRATOR" == "administrator"

This is convenient for an untyped property bag, but the semantics can vary depending on the data being compared.

### FilterSchema

A `FilterSchema` declares how properties should be compared.

Example:
```Java
    FilterSchema schema = FilterSchema.builder()
    .numeric("age")
    .caseExact("identifier")
    .build();
```

This ensures that:
```Java
schema.greaterThan("age", "30");
```
always performs a numeric comparison, while:

```Java
schema.equalTo("identifier", "ABC123");
```
always performs a case-sensitive comparison.


### Custom matching rules

Third parties may provide their own comparison semantics by implementing MatchingRule.

Example use cases:

    Version numbers
    Locale-aware sorting
    Domain-specific identifiers

The comparison behavior becomes part of the filter itself, ensuring that all consumers interpret it consistently.

### Parsing and Rendering

Parsing was optional in the specs but it enables logging and testing.

Every core filter can be rendered into a canonical string representation:
```Java
String rendered = filter.toFilterString();
```

And later reconstructed by parsing that representation:
```Java
Filter parsed = Filter.parse(rendered);
```

## Future Enhancements

### Filter limits
The API needs filter limits to prevent malicious filters from blowing up the stack.

### Semantic capabilities as interfaces
Canonical semantic visitors may have limited or no support for extension filter types.
Although generic visitors can traverse an extension's nested operands, the `ExtensibleFilter` contract does not expose enough information to interpret the extension's own semantics. 
Attempting to do so could produce silently incomplete results.

A future version could introduce optional semantic capability interfaces, such as a contract for exposing referenced resource properties. 
Participating extensions could implement these capabilities to allow canonical visitors to process them without depending on concrete extension types.

Domain-specific extension families could instead define their own visitor interfaces, providing exhaustive type-safe dispatch within the set of extension types controlled by that family.

Example:

A semantic capability for PropertyReferences could be introduced that extension filters could implement
to return every resource property the filter references.

That filter would implement both ExtensibleFilter and PropertyReferences and override a get properties method in addition
to the existing extensionId, parameters, and operands methods.

The PropertyCollector would check if the filter implements PropertyReferences, else throw an error.


## Use of AI

AI assistance was used during development, particularly when implementing the optional parsing functionality, exploring API design alternatives, and helping write tests.

One of the more challenging design areas was requirement 5:

    5a -> Allow third parties to introduce new filter types.
    5b -> Allow third parties to perform type-safe processing of filters.

An exhaustive type-safe visitor requires every filter type to have a dedicated visitor method.
But extension filter types are open-ended and may not exist when a visitor is compiled.

AI was used to explore these design tradeoffs and evaluate alternative approaches.

All code, documentation, and tests were reviewed and validated manually.

Final design and implementation decisions were made by the author.