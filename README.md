# `mylang2IR` - CMPE 230: Systems Programming Homework 1

In this project, a translator called mylang2IR is implemented, which takes a myLang script (.my file) as input and returns LLVM-IR script (.ll file) as an output.

## Details of `myLang` Language
MyLang language statements will be as follows:

1. Variables are integer variables. The default values of the varaibles are 0 (i.e. if they are not initialized).
2. Executable statements consist of one-line statements, while-loop, and if compound statements. No nested while-loops or if statements are allowed.
3. One-line statements are either assignment statements or print statements that print the value of an expression.
4. There is a function `choose(expr1, expr2, expr3, expr4)` which returns `expr2` if `expr1` is equal to 0, returns `expr3` if `expr1` is positive and returns `expr4` if `expr1` is negative.
5. The operations in expressions are multiplication, division, addition and subtraction: \*, /, +, -. These are binary operand oeprations. Unary minus is *not* supported, but parenthese are allowed. Operator precedence is as in other programming languages, such as C or Java.
6. On a line, everything after \# sign is considered as comments.
7. If statement has the following format:
        
        if (expr) {
            .....
            .....
        }
8. While loop has the following format:
        
        while (expr) {
            .....
            .....
        }
9. If `expr` has a nonzero value, it means true. If `expr` has zero value, it means false. There are no nested while statements.
10. `print(id)` statement prints the value of variable `id`.
