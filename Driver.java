/**
 * REPL driver for a lambda interpreter.
 */

import java.util.Scanner;

public class Driver {
    public static void main(String[] args) {
        // Set up the environment
        AstEnvironment env = new AstEnvironment();

        // Start off the REPL
        Scanner keyboard = new Scanner(System.in);
        System.out.print("> ");
        while(keyboard.hasNextLine()) {
            String lineInput = keyboard.nextLine();
            Lexer lexer = new Lexer(lineInput);
            try {
                // Parse the program
                AstProgram program = parseProgram(lexer);

                // Evaluate the resultant expression
                AstNode result = program.evaluate(env);

                // Print the reduced lambda expression
                System.out.println(result);
            } catch (ParseException pe) {
                System.out.println("Parsing error: " + pe.getMessage());
            } finally {
                // Wait for the next input
                System.out.println();
                System.out.print("> ");
            }
        }
    }

    /**
     * Parses a program.
     * Grammar Rules:
     * P -> E, where E is an expression.
     * P -> A, where A is an assignment.
     *
     * @param   input   the lexer/scanner for the current program pass
     * @return          a program node representing the parsed program
     */
    public static AstProgram parseProgram(Lexer input) throws ParseException {
        AstProgram program;
        if (input.peek() == Token.MACRO) {
            // Current case: Parsing an assignment
            AstAssignment assignment = parseAssignment(input);
            program = new AstProgram(assignment);
        } else {
            // Current case: Parsing an expression
            AstExpression expression = parseExpression(input);
            program = new AstProgram(expression);
        }

        // Check that we have reached the end of the input
        Token token = input.next();
        if (token != Token.EOF) {
            throw new ParseException("end of input", input.getLastToken());
        }

        return program;
    }

    /**
     * Parses an input assignment.
     * Grammar Rules:
     * A -> M := E, where M is a macro, E is an expression.
     *
     * @param   input   the lexer/scanner for the current program pass
     * @return          an assignment node representing the parsed assignment
     */
    public static AstAssignment parseAssignment(Lexer input) throws ParseException {
        Token token = input.next();

        if (token == Token.MACRO) {
            // A -> M := E

            // Parse the macro M
            AstMacro macro = new AstMacro(input.getIdentifier());

            // Parse the token ':='
            token = input.next();
            if (token != Token.OP_ASSIGNMENT) {
                throw new ParseException(":=", input.getLastToken());
            }

            // Parse the expression E
            AstExpression rhs = parseExpression(input);

            return new AstAssignment(macro, rhs);
        } else {
            throw new ParseException("assignment", input.getLastToken());
        }
    }

    /**
     * Parses an input expression.
     * Grammar Rules:
     * E -> (EE) - an application
     * E -> \x.E - an abstraction
     * E -> V    - a variable symbol
     * E -> M    - a macro symbol
     * E -> (E)  - a bracketed expression
     *
     * @param   input   the lexer/scanner for the current program pass
     * @return          an expression node representing the parsed expression
     */
    public static AstExpression parseExpression(Lexer input) throws ParseException {
        Token token = input.next();

        if (token == Token.LBRACKET) {
            // Two possible cases: E -> (EE) | (E)

            // Parse the first expression
            AstExpression left = parseExpression(input);

            // Check if we have another expression to parse
            if (input.peek() == Token.RBRACKET) {
                // Current case: E -> (E)
                // read off the right bracket
                input.next();

                return left;
            }

            // Current case: E -> (EE)
            // Parse the remaining expression
            AstExpression right = parseExpression(input);

            // TODO: change this to possibly read in (E^n) with left-associativity
            // Parse )
            token = input.next();
            if(token != Token.RBRACKET) {
                throw new ParseException("')'", input.getLastToken());
            }

            return new AstApplication(left, right);
        } else if (token == Token.LAMBDA) {
            // E -> \var.E

            // Parse the head variable
            token = input.next();
            if(token != Token.VARIABLE) {
                throw new ParseException("variable", input.getLastToken());
            }
            AstVariable variable = new AstVariable(input.getIdentifier());

            // Parse '.'
            token = input.next();
            if(token != Token.DOT) {
                throw new ParseException("'.'", input.getLastToken());
            }

            // Parse the expression E
            AstExpression subExpression = parseExpression(input);

            return new AstAbstraction(variable, subExpression);
        } else if (token == Token.VARIABLE) {
            // E -> V
            return new AstVariable(input.getIdentifier());
        } else if (token == Token.MACRO) {
            // E -> M
            return new AstMacro(input.getIdentifier());
        } else {
            throw new ParseException("expression", input.getLastToken());
        }
    }
}
