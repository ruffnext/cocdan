pub struct Dice (String);

impl Default for Dice {
    fn default() -> Self {
        return Self("".to_string())
    }
}
