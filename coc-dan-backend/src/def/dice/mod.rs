#[allow(unused)]
mod parser;
pub mod def;

#[cfg(test)]
mod tests {
    use crate::def::dice::parser::ExprParser;

    #[test]
    fn test_dice_parser() {
        let parser = ExprParser::new();
        assert_eq!(parser.parse("-3D1 * 5").unwrap(), -15.0);
    }
}
