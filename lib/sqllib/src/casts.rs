//! Implementation of various cast operations.
// Map of type names
// * Bool -> b
// * Date -> date      (no implementation yet)
// * Decimal -> decimal
// * Double -> d
// * Float -> f
// * GeoPoint -> geopoint
// * Null -> null
// * String -> s
// * Timestamp -> Timestamp
// * Interval -> ShortInteval or LongInterval
// * isize -> i         (not a SQL type, never nullable)
// * signed16 -> i16
// * signed32 -> i32
// * signed64 -> i64
// * str -> str         (not a SQL type, never nullable)
// * usize -> u         (not a SQL type, never nullable)

#![allow(non_snake_case)]

use dbsp::algebra::{F32, F64, HasZero, HasOne};
use rust_decimal::prelude::*;
use rust_decimal_macros::dec;
use crate::{
    interval::*,
    geopoint::*,
};

/////////// cast to b

#[inline]
pub fn cast_to_b_b(value: bool) -> bool
{
    value
}

#[inline]
pub fn cast_to_b_bN(value: Option<bool>) -> bool
{
    value.unwrap()
}

#[inline]
pub fn cast_to_b_decimal(value: Decimal) -> bool
{
    value != dec!(0)
}

#[inline]
pub fn cast_to_b_decimalN(value: Option<Decimal>) -> bool
{
    value.unwrap() != dec!(0)
}

#[inline]
pub fn cast_to_b_d(value: F64) -> bool
{
    value != 0.0
}

#[inline]
pub fn cast_to_b_dN(value: Option<F64>) -> bool
{
    value.unwrap() != F64::zero()
}

#[inline]
pub fn cast_to_b_f(value: F32) -> bool
{
    value != F32::zero()
}

#[inline]
pub fn cast_to_b_fN(value: Option<F32>) -> bool
{
    value.unwrap() != F32::zero()
}

#[inline]
pub fn cast_to_b_s(value: String) -> bool
{
    match value.parse() {
        Err(_) => false,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_b_sN(value: Option<String>) -> bool
{
    match value.unwrap().parse() {
        Err(_) => false,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_b_i(value: isize) -> bool
{
    value != 0
}

#[inline]
pub fn cast_to_b_i16(value: i16) -> bool
{
    value != 0
}

#[inline]
pub fn cast_to_b_i16N(value: Option<i16>) -> bool
{
    value.unwrap() != 0
}

#[inline]
pub fn cast_to_b_i32(value: i32) -> bool
{
    value != 0
}

#[inline]
pub fn cast_to_b_i32N(value: Option<i32>) -> bool
{
    value.unwrap() != 0
}

#[inline]
pub fn cast_to_b_i64(value: i64) -> bool
{
    value != 0
}

#[inline]
pub fn cast_to_b_i64N(value: Option<i64>) -> bool
{
    value.unwrap() != 0
}

#[inline]
pub fn cast_to_b_u(value: usize) -> bool
{
    value != 0
}

/////////// cast to bN

#[inline]
pub fn cast_to_bN_b(value: bool) -> Option<bool>
{
    Some(value)
}

#[inline]
pub fn cast_to_bN_bN(value: Option<bool>) -> Option<bool>
{
    value
}

#[inline]
pub fn cast_to_bN_decimal(value: Decimal) -> Option<bool>
{
    Some(value != dec!(0))
}

#[inline]
pub fn cast_to_bN_decimalN(value: Option<Decimal>) -> Option<bool>
{
    value.map(|x| x != dec!(0))
}

#[inline]
pub fn cast_to_bN_d(value: F64) -> Option<bool>
{
    Some(value != F64::zero())
}

#[inline]
pub fn cast_to_bN_dN(value: Option<F64>) -> Option<bool>
{
    value.map(|x| x != F64::zero())
}

#[inline]
pub fn cast_to_bN_f(value: F32) -> Option<bool>
{
    Some(value != F32::zero())
}

#[inline]
pub fn cast_to_bN_fN(value: Option<F32>) -> Option<bool>
{
    value.map(|x| x != F32::zero())
}

#[inline]
pub fn cast_to_bN_s(value: String) -> Option<bool>
{
    match value.parse() {
        Err(_) => Some(false),
        Ok(x) => Some(x),
    }
}

#[inline]
pub fn cast_to_bN_sN(value: Option<String>) -> Option<bool>
{
    match value {
        None => None,
        Some(x) => match x.parse() {
            Err(_) => Some(false),
            Ok(y) => Some(y),
        }
    }
}

#[inline]
pub fn cast_to_bN_i(value: isize) -> Option<bool>
{
    Some(value != 0)
}

#[inline]
pub fn cast_to_bN_i16(value: i16) -> Option<bool>
{
    Some(value != 0)
}

#[inline]
pub fn cast_to_bN_i16N(value: Option<i16>) -> Option<bool>
{
    value.map(|x| x != 0)
}

#[inline]
pub fn cast_to_bN_i32(value: i32) -> Option<bool>
{
    Some(value != 0)
}

#[inline]
pub fn cast_to_bN_i32N(value: Option<i32>) -> Option<bool>
{
    value.map(|x| x != 0)
}

#[inline]
pub fn cast_to_bN_i64(value: i64) -> Option<bool>
{
    Some(value != 0)
}

#[inline]
pub fn cast_to_bN_i64N(value: Option<i64>) -> Option<bool>
{
    value.map(|x| x != 0)
}

#[inline]
pub fn cast_to_bN_u(value: usize) -> Option<bool>
{
    Some(value != 0)
}

/////////// cast to date

// TODO

/////////// cast to dateN

// TODO

/////////// cast to decimal

#[inline]
pub fn cast_to_decimal_b(value: bool) -> Decimal
{
    if value { Decimal::one() } else { Decimal::zero() }
}

#[inline]
pub fn cast_to_decimal_bN(value: Option<bool>) -> Decimal
{
    if value.unwrap() {Decimal::one()} else {Decimal::zero()}
}

#[inline]
pub fn cast_to_decimal_decimal(value: Decimal) -> Decimal
{
    value
}

#[inline]
pub fn cast_to_decimal_decimalN(value: Option<Decimal>) -> Decimal
{
    value.unwrap()
}

#[inline]
pub fn cast_to_decimal_d(value: F64) -> Decimal
{
    Decimal::from_f64(value.into_inner()).unwrap()
}

#[inline]
pub fn cast_to_decimal_dN(value: Option<F64>) -> Decimal
{
    Decimal::from_f64(value.unwrap().into_inner()).unwrap()
}

#[inline]
pub fn cast_to_decimal_f(value: F32) -> Decimal
{
    Decimal::from_f32(value.into_inner()).unwrap()
}

#[inline]
pub fn cast_to_decimal_fN(value: Option<F32>) -> Decimal
{
    Decimal::from_f32(value.unwrap().into_inner()).unwrap()
}

#[inline]
pub fn cast_to_decimal_s(value: String) -> Decimal
{
    match value.parse().ok() {
        None => Decimal::zero(),
        Some(x) => x,
    }
}

#[inline]
pub fn cast_to_decimal_sN(value: Option<String>) -> Decimal
{
    match value {
        None => Decimal::zero(),
        Some(x) => match x.parse().ok() {
            None => Decimal::zero(),
            Some(x) => x,
        },
    }
}

#[inline]
pub fn cast_to_decimal_i(value: isize) -> Decimal
{
    Decimal::from_isize(value).unwrap()
}

#[inline]
pub fn cast_to_decimal_i16(value: i16) -> Decimal
{
    Decimal::from_i16(value).unwrap()
}

#[inline]
pub fn cast_to_decimal_i16N(value: Option<i16>) -> Decimal
{
    Decimal::from_i16(value.unwrap()).unwrap()
}

#[inline]
pub fn cast_to_decimal_i32(value: i32) -> Decimal
{
    Decimal::from_i32(value).unwrap()
}

#[inline]
pub fn cast_to_decimal_i32N(value: Option<i32>) -> Decimal
{
    Decimal::from_i32(value.unwrap()).unwrap()
}

#[inline]
pub fn cast_to_decimal_i64(value: i64) -> Decimal
{
    Decimal::from_i64(value).unwrap()
}

#[inline]
pub fn cast_to_decimal_i64N(value: Option<i64>) -> Decimal
{
    Decimal::from_i64(value.unwrap()).unwrap()
}

#[inline]
pub fn cast_to_decimal_u(value: usize) -> Decimal
{
    Decimal::from_usize(value).unwrap()
}

/////////// cast to decimalN

#[inline]
pub fn cast_to_decimalN_b(value: bool) -> Option<Decimal>
{
    if value { Some(Decimal::one()) } else { Some(Decimal::zero()) }
}

#[inline]
pub fn cast_to_decimalN_bN(value: Option<bool>) -> Option<Decimal>
{
    value.map(|x| if x {Decimal::one()} else {Decimal::zero()})
}

#[inline]
pub fn cast_to_decimalN_decimal(value: Decimal) -> Option<Decimal>
{
    Some(value)
}

#[inline]
pub fn cast_to_decimalN_decimalN(value: Option<Decimal>) -> Option<Decimal>
{
    value
}

#[inline]
pub fn cast_to_decimalN_d(value: F64) -> Option<Decimal>
{
    Decimal::from_f64(value.into_inner())
}

#[inline]
pub fn cast_to_decimalN_dN(value: Option<F64>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => Decimal::from_f64(x.into_inner()),
    }
}

#[inline]
pub fn cast_to_decimalN_f(value: F32) -> Option<Decimal>
{
    Decimal::from_f32(value.into_inner())
}

#[inline]
pub fn cast_to_decimalN_fN(value: Option<F32>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => Decimal::from_f32(x.into_inner()),
    }
}

#[inline]
pub fn cast_to_decimalN_s(value: String) -> Option<Decimal>
{
    match value.parse() {
        Err(_) => Some(Decimal::zero()),
        Ok(x) => Some(x),
    }
}

#[inline]
pub fn cast_to_decimalN_sN(value: Option<String>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => match x.parse() {
            Err(_) => Some(Decimal::zero()),
            Ok(y) => Some(y),
        },
    }
}

#[inline]
pub fn cast_to_decimalN_i(value: isize) -> Option<Decimal>
{
    Decimal::from_isize(value)
}

#[inline]
pub fn cast_to_decimalN_i16(value: i16) -> Option<Decimal>
{
    Decimal::from_i16(value)
}

#[inline]
pub fn cast_to_decimalN_i16N(value: Option<i16>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => Decimal::from_i16(x),
    }
}

#[inline]
pub fn cast_to_decimalN_i32(value: i32) -> Option<Decimal>
{
    Decimal::from_i32(value)
}

#[inline]
pub fn cast_to_decimalN_i32N(value: Option<i32>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => Decimal::from_i32(x),
    }
}

#[inline]
pub fn cast_to_decimalN_i64(value: i64) -> Option<Decimal>
{
    Decimal::from_i64(value)
}

#[inline]
pub fn cast_to_decimalN_i64N(value: Option<i64>) -> Option<Decimal>
{
    match value {
        None => None,
        Some(x) => Decimal::from_i64(x),
    }
}

#[inline]
pub fn cast_to_decimalN_u(value: usize) -> Option<Decimal>
{
    Decimal::from_usize(value)
}

/////////// cast to double

#[inline]
pub fn cast_to_d_b(value: bool) -> F64
{
    if value { F64::one() } else { F64::zero() }
}

#[inline]
pub fn cast_to_d_bN(value: Option<bool>) -> F64
{
    if value.unwrap() { F64::one() } else { F64::zero() }
}

#[inline]
pub fn cast_to_d_decimal(value: Decimal) -> F64
{
    F64::from(value.to_f64().unwrap())
}

#[inline]
pub fn cast_to_d_decimalN(value: Option<Decimal>) -> F64
{
    F64::from(value.unwrap().to_f64().unwrap())
}

#[inline]
pub fn cast_to_d_d(value: F64) -> F64
{
    value
}

#[inline]
pub fn cast_to_d_dN(value: Option<F64>) -> F64
{
    value.unwrap()
}

#[inline]
pub fn cast_to_d_f(value: F32) -> F64
{
    F64::from(value.into_inner())
}

#[inline]
pub fn cast_to_d_fN(value: Option<F32>) -> F64
{
    F64::from(value.unwrap().into_inner())
}

#[inline]
pub fn cast_to_d_s(value: String) -> F64
{
    match value.parse() {
        Err(_) => F64::zero(),
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_d_sN(value: Option<String>) -> F64
{
    match value.unwrap().parse() {
        Err(_) => F64::zero(),
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_d_i(value: isize) -> F64
{
    F64::from(value as f64)
}

#[inline]
pub fn cast_to_d_i16(value: i16) -> F64
{
    F64::from(value)
}

#[inline]
pub fn cast_to_d_i16N(value: Option<i16>) -> F64
{
    F64::from(value.unwrap())
}

#[inline]
pub fn cast_to_d_i32(value: i32) -> F64
{
    F64::from(value)
}

#[inline]
pub fn cast_to_d_i32N(value: Option<i32>) -> F64
{
    F64::from(value.unwrap())
}

#[inline]
pub fn cast_to_d_i64(value: i64) -> F64
{
    F64::from(value as f64)
}

#[inline]
pub fn cast_to_d_i64N(value: Option<i64>) -> F64
{
    F64::from(value.unwrap() as f64)
}

#[inline]
pub fn cast_to_d_u(value: usize) -> F64
{
    F64::from(value as f64)
}

/////////// cast to doubleN

#[inline]
pub fn cast_to_dN_b(value: bool) -> Option<F64>
{
    if value { Some(F64::one()) } else { Some(F64::zero()) }
}

#[inline]
pub fn cast_to_dN_bN(value: Option<bool>) -> Option<F64>
{
    value.map(|x| if x {F64::one()} else {F64::zero()})
}

#[inline]
pub fn cast_to_dN_decimal(value: Decimal) -> Option<F64>
{
    value.to_f64().map(|x| F64::from(x))
}

#[inline]
pub fn cast_to_dN_decimalN(value: Option<Decimal>) -> Option<F64>
{
    match value {
        None => None,
        Some(x) => x.to_f64().map(|y| F64::from(y)),
    }
}

#[inline]
pub fn cast_to_dN_d(value: F64) -> Option<F64>
{
    Some(value)
}

#[inline]
pub fn cast_to_dN_dN(value: Option<F64>) -> Option<F64>
{
    value
}

#[inline]
pub fn cast_to_dN_f(value: F32) -> Option<F64>
{
    Some(F64::from(value.into_inner()))
}

#[inline]
pub fn cast_to_dN_fN(value: Option<F32>) -> Option<F64>
{
    value.map(|x| F64::from(x.into_inner()))
}

#[inline]
pub fn cast_to_dN_s(value: String) -> Option<F64>
{
    match value.parse::<f64>() {
        Err(_) => Some(F64::zero()),
        Ok(x) => Some(F64::new(x)),
    }
}

#[inline]
pub fn cast_to_dN_sN(value: Option<String>) -> Option<F64>
{
    match value {
        None => None,
        Some(x) => match x.parse::<f64>() {
            Err(_) => Some(F64::zero()),
            Ok(x) => Some(F64::new(x)),
        },
    }
}

#[inline]
pub fn cast_to_dN_i(value: isize) -> Option<F64>
{
    Some(F64::from(value as f64))
}

#[inline]
pub fn cast_to_dN_i16(value: i16) -> Option<F64>
{
    Some(F64::from(value as f64))
}

#[inline]
pub fn cast_to_dN_i16N(value: Option<i16>) -> Option<F64>
{
    value.map(|x| F64::from(x))
}

#[inline]
pub fn cast_to_dN_i32(value: i32) -> Option<F64>
{
    Some(F64::from(value))
}

#[inline]
pub fn cast_to_dN_i32N(value: Option<i32>) -> Option<F64>
{
    value.map(|x| F64::from(x))
}

#[inline]
pub fn cast_to_dN_i64(value: i64) -> Option<F64>
{
    Some(F64::from(value as f64))
}

#[inline]
pub fn cast_to_dN_i64N(value: Option<i64>) -> Option<F64>
{
    value.map(|x| F64::from(x as f64))
}

#[inline]
pub fn cast_to_dN_u(value: usize) -> Option<F64>
{
    Some(F64::from(value as f64))
}

/////////// Cast to float

#[inline]
pub fn cast_to_f_b(value: bool) -> F32
{
    if value { F32::one() } else { F32::zero() }
}

#[inline]
pub fn cast_to_f_bN(value: Option<bool>) -> F32
{
    if value.unwrap() { F32::one() } else { F32::zero() }
}

#[inline]
pub fn cast_to_f_decimal(value: Decimal) -> F32
{
    F32::from(value.to_f32().unwrap())
}

#[inline]
pub fn cast_to_f_decimalN(value: Option<Decimal>) -> F32
{
    F32::from(value.unwrap().to_f32().unwrap())
}

#[inline]
pub fn cast_to_f_d(value: F64) -> F32
{
    F32::from(value.into_inner() as f32)
}

#[inline]
pub fn cast_to_f_dN(value: Option<F64>) -> F32
{
    F32::from(value.unwrap().into_inner() as f32)
}

#[inline]
pub fn cast_to_f_f(value: F32) -> F32
{
    value
}

#[inline]
pub fn cast_to_f_fN(value: Option<F32>) -> F32
{
    value.unwrap()
}

#[inline]
pub fn cast_to_f_s(value: String) -> F32
{
    match value.parse() {
        Err(_) => F32::zero(),
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_f_sN(value: Option<String>) -> F32
{
    match value.unwrap().parse()  {
        Err(_) => F32::zero(),
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_f_i(value: isize) -> F32
{
    F32::from(value as f32)
}

#[inline]
pub fn cast_to_f_i16(value: i16) -> F32
{
    F32::from(value)
}

#[inline]
pub fn cast_to_f_i16N(value: Option<i16>) -> F32
{
    F32::from(value.unwrap())
}

#[inline]
pub fn cast_to_f_i32(value: i32) -> F32
{
    F32::from(value as f32)
}

#[inline]
pub fn cast_to_f_i32N(value: Option<i32>) -> F32
{
    F32::from(value.unwrap() as f32)
}

#[inline]
pub fn cast_to_f_i64(value: i64) -> F32
{
    F32::from(value as f32)
}

#[inline]
pub fn cast_to_f_i64N(value: Option<i64>) -> F32
{
    F32::from(value.unwrap() as f32)
}

#[inline]
pub fn cast_to_f_u(value: usize) -> F32
{
    F32::from(value as f32)
}

/////////// cast to floatN

#[inline]
pub fn cast_to_fN_b(value: bool) -> Option<F32>
{
    if value { Some(F32::one()) } else { Some(F32::zero()) }
}

#[inline]
pub fn cast_to_fN_bN(value: Option<bool>) -> Option<F32>
{
    value.map(|x| if x {F32::one()} else {F32::zero()})
}

#[inline]
pub fn cast_to_fN_decimal(value: Decimal) -> Option<F32>
{
    value.to_f32().map(|x| F32::from(x))
}

#[inline]
pub fn cast_to_fN_decimalN(value: Option<Decimal>) -> Option<F32>
{
    match value {
        None => None,
        Some(x) => x.to_f32().map(|y| F32::from(y)),
    }
}

#[inline]
pub fn cast_to_fN_d(value: F64) -> Option<F32>
{
    Some(F32::from(value.into_inner() as f32))
}

#[inline]
pub fn cast_to_fN_dN(value: Option<F64>) -> Option<F32>
{
    value.map(|x| F32::from(x.into_inner() as f32))
}

#[inline]
pub fn cast_to_fN_f(value: F32) -> Option<F32>
{
    Some(value)
}

#[inline]
pub fn cast_to_fN_fN(value: Option<F32>) -> Option<F32>
{
    value
}

#[inline]
pub fn cast_to_fN_s(value: String) -> Option<F32>
{
    match value.parse::<f32>() {
        Err(_) => Some(F32::zero()),
        Ok(x) => Some(F32::from(x)),
    }
}

#[inline]
pub fn cast_to_fN_sN(value: Option<String>) -> Option<F32>
{
    match value {
        None => None,
        Some(x) => match x.parse::<f32>() {
            Err(_) => Some(F32::zero()),
            Ok(x) => Some(F32::from(x)),
        }
    }
}

#[inline]
pub fn cast_to_fN_i(value: isize) -> Option<F32>
{
    Some(F32::from(value as f32))
}

#[inline]
pub fn cast_to_fN_i16(value: i16) -> Option<F32>
{
    Some(F32::from(value as f32))
}

#[inline]
pub fn cast_to_fN_i16N(value: Option<i16>) -> Option<F32>
{
    value.map(|x| F32::from(x))
}

#[inline]
pub fn cast_to_fN_i32(value: i32) -> Option<F32>
{
    Some(F32::from(value as f32))
}

#[inline]
pub fn cast_to_fN_i32N(value: Option<i32>) -> Option<F32>
{
    value.map(|x| F32::from(x as f32))
}

#[inline]
pub fn cast_to_fN_i64(value: i64) -> Option<F32>
{
    Some(F32::from(value as f32))
}

#[inline]
pub fn cast_to_fN_i64N(value: Option<i64>) -> Option<F32>
{
    value.map(|x| F32::from(x as f32))
}

#[inline]
pub fn cast_to_fN_u(value: usize) -> Option<F32>
{
    Some(F32::from(value as f32))
}

/////////// cast to GeoPoint

#[inline]
pub fn cast_to_geopointN_geopoint(value: GeoPoint) -> Option<GeoPoint>
{
    Some(value)
}

/////////// cast to String

#[inline]
pub fn s_helper<T>(value: Option<T>) -> String
where T: ToString
{
    match value {
        None => String::from("NULL"),
        Some(x) => x.to_string(),
    }
}

#[inline]
pub fn cast_to_s_b(value: bool) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_bN(value: Option<bool>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_decimal(value: Decimal) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_decimalN(value: Option<Decimal>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_d(value: F64) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_dN(value: Option<F64>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_f(value: F32) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_fN(value: Option<F32>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_s(value: String) -> String
{
    value
}

#[inline]
pub fn cast_to_s_sN(value: Option<String>) -> String
{
    value.unwrap()
}

#[inline]
pub fn cast_to_s_i(value: isize) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_i16(value: i16) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_i16N(value: Option<i16>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_i32(value: i32) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_i32N(value: Option<i32>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_i64(value: i64) -> String
{
    value.to_string()
}

#[inline]
pub fn cast_to_s_i64N(value: Option<i64>) -> String
{
    s_helper(value)
}

#[inline]
pub fn cast_to_s_u(value: usize) -> String
{
    value.to_string()
}

/////////// cast to StringN

#[inline]
pub fn sN_helper<T>(value: Option<T>) -> Option<String>
where T: ToString
{
    value.map(|x| x.to_string())
}

#[inline]
pub fn cast_to_sN_b(value: bool) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_bN(value: Option<bool>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_decimal(value: Decimal) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_decimalN(value: Option<Decimal>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_d(value: F64) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_dN(value: Option<F64>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_f(value: F32) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_fN(value: Option<F32>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_s(value: String) -> Option<String>
{
    Some(value)
}

#[inline]
pub fn cast_to_sN_sN(value: Option<String>) -> Option<String>
{
    value
}

#[inline]
pub fn cast_to_sN_i(value: isize) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_i16(value: i16) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_i16N(value: Option<i16>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_i32(value: i32) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_i32N(value: Option<i32>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_i64(value: i64) -> Option<String>
{
    Some(value.to_string())
}

#[inline]
pub fn cast_to_sN_i64N(value: Option<i64>) -> Option<String>
{
    sN_helper(value)
}

#[inline]
pub fn cast_to_sN_u(value: usize) -> Option<String>
{
    Some(value.to_string())
}

/////////// cast to Timestamp

// TODO

/////////// cast to TimestampN

// TODO

/////////// cast to i16

#[inline]
pub fn cast_to_i16_b(value: bool) -> i16
{
    if value { 1 } else { 0 }
}

#[inline]
pub fn cast_to_i16_bN(value: Option<bool>) -> i16
{
    if value.unwrap() {1} else {0}
}

#[inline]
pub fn cast_to_i16_decimal(value: Decimal) -> i16
{
    value.to_i16().unwrap()
}

#[inline]
pub fn cast_to_i16_decimalN(value: Option<Decimal>) -> i16
{
    value.unwrap().to_i16().unwrap()
}

#[inline]
pub fn cast_to_i16_d(value: F64) -> i16
{
    value.into_inner() as i16
}

#[inline]
pub fn cast_to_i16_dN(value: Option<F64>) -> i16
{
    value.unwrap().into_inner() as i16
}

#[inline]
pub fn cast_to_i16_f(value: F32) -> i16
{
    value.into_inner() as i16
}

#[inline]
pub fn cast_to_i16_fN(value: Option<F32>) -> i16
{
    value.unwrap().into_inner() as i16
}

#[inline]
pub fn cast_to_i16_s(value: String) -> i16
{
    match value.parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i16_sN(value: Option<String>) -> i16
{
    match value.unwrap().parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i16_i(value: isize) -> i16
{
    value as i16
}

#[inline]
pub fn cast_to_i16_i16(value: i16) -> i16
{
    value
}

#[inline]
pub fn cast_to_i16_i16N(value: Option<i16>) -> i16
{
    value.unwrap()
}

#[inline]
pub fn cast_to_i16_i32(value: i32) -> i16
{
    value as i16
}

#[inline]
pub fn cast_to_i16_i32N(value: Option<i32>) -> i16
{
    value.unwrap() as i16
}

#[inline]
pub fn cast_to_i16_i64(value: i64) -> i16
{
    value as i16
}

#[inline]
pub fn cast_to_i16_i64N(value: Option<i64>) -> i16
{
    value.unwrap() as i16
}

#[inline]
pub fn cast_to_i16_u(value: usize) -> i16
{
    value as i16
}

/////////// cast to i16N

#[inline]
pub fn cast_to_i16N_b(value: bool) -> Option<i16>
{
    if value { Some(1) } else { Some(0) }
}

#[inline]
pub fn cast_to_i16N_bN(value: Option<bool>) -> Option<i16>
{
    value.map(|x| if x {1} else {0})
}

#[inline]
pub fn cast_to_i16N_decimal(value: Decimal) -> Option<i16>
{
    value.to_i16()
}

#[inline]
pub fn cast_to_i16N_decimalN(value: Option<Decimal>) -> Option<i16>
{
    match value {
        None => None,
        Some(x) => x.to_i16(),
    }
}

#[inline]
pub fn cast_to_i16N_d(value: F64) -> Option<i16>
{
    Some(value.into_inner() as i16)
}

#[inline]
pub fn cast_to_i16N_dN(value: Option<F64>) -> Option<i16>
{
    value.map(|x| x.into_inner() as i16)
}

#[inline]
pub fn cast_to_i16N_f(value: F32) -> Option<i16>
{
    Some(value.into_inner() as i16)
}

#[inline]
pub fn cast_to_i16N_fN(value: Option<F32>) -> Option<i16>
{
    value.map(|x| x.into_inner() as i16)
}

#[inline]
pub fn cast_to_i16N_s(value: String) -> Option<i16>
{
    match value.parse() {
        Err(_) => Some(0),
        Ok(x) => Some(x),
    }
}

#[inline]
pub fn cast_to_i16N_sN(value: Option<String>) -> Option<i16>
{
    match value {
        None => None,
        Some(x) => match x.parse() {
            Err(_) => Some(0),
            Ok(y) => Some(y),
        },
    }
}

#[inline]
pub fn cast_to_i16N_i(value: isize) -> Option<i16>
{
    Some(value as i16)
}

#[inline]
pub fn cast_to_i16N_i16(value: i16) -> Option<i16>
{
    Some(value)
}

#[inline]
pub fn cast_to_i16N_i16N(value: Option<i16>) -> Option<i16>
{
    value
}

#[inline]
pub fn cast_to_i16N_i32(value: i32) -> Option<i16>
{
    Some(value as i16)
}

#[inline]
pub fn cast_to_i16N_i32N(value: Option<i32>) -> Option<i16>
{
    value.map(|x| x as i16)
}

#[inline]
pub fn cast_to_i16N_i64(value: i64) -> Option<i16>
{
    Some(value as i16)
}

#[inline]
pub fn cast_to_i16N_i64N(value: Option<i64>) -> Option<i16>
{
    value.map(|x| x as i16)
}

#[inline]
pub fn cast_to_i16N_u(value: usize) -> Option<i16>
{
    Some(value as i16)
}

/////////// cast to i32

#[inline]
pub fn cast_to_i32_b(value: bool) -> i32
{
    if value { 1 } else { 0 }
}

#[inline]
pub fn cast_to_i32_bN(value: Option<bool>) -> i32
{
    if value.unwrap() {1} else {0}
}

#[inline]
pub fn cast_to_i32_decimal(value: Decimal) -> i32
{
    value.to_i32().unwrap()
}

#[inline]
pub fn cast_to_i32_decimalN(value: Option<Decimal>) -> i32
{
    value.unwrap().to_i32().unwrap()
}

#[inline]
pub fn cast_to_i32_d(value: F64) -> i32
{
    value.into_inner() as i32
}

#[inline]
pub fn cast_to_i32_dN(value: Option<F64>) -> i32
{
    value.unwrap().into_inner() as i32
}

#[inline]
pub fn cast_to_i32_f(value: F32) -> i32
{
    value.into_inner() as i32
}

#[inline]
pub fn cast_to_i32_fN(value: Option<F32>) -> i32
{
    value.unwrap().into_inner() as i32
}

#[inline]
pub fn cast_to_i32_s(value: String) -> i32
{
    match value.parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i32_sN(value: Option<String>) -> i32
{
    match value.unwrap().parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i32_i(value: isize) -> i32
{
    value as i32
}

#[inline]
pub fn cast_to_i32_i16(value: i16) -> i32
{
    value as i32
}

#[inline]
pub fn cast_to_i32_i16N(value: Option<i16>) -> i32
{
    value.unwrap() as i32
}

#[inline]
pub fn cast_to_i32_i32(value: i32) -> i32
{
    value
}

#[inline]
pub fn cast_to_i32_i32N(value: Option<i32>) -> i32
{
    value.unwrap()
}

#[inline]
pub fn cast_to_i32_i64(value: i64) -> i32
{
    value as i32
}

#[inline]
pub fn cast_to_i32_i64N(value: Option<i64>) -> i32
{
    value.unwrap() as i32
}

#[inline]
pub fn cast_to_i32_u(value: usize) -> i32
{
    value as i32
}

/////////// cast to i32N

#[inline]
pub fn cast_to_i32N_b(value: bool) -> Option<i32>
{
    if value { Some(1) } else { Some(0) }
}

#[inline]
pub fn cast_to_i32N_bN(value: Option<bool>) -> Option<i32>
{
    value.map(|x| if x {1} else {0})
}

#[inline]
pub fn cast_to_i32N_decimal(value: Decimal) -> Option<i32>
{
    value.to_i32()
}

#[inline]
pub fn cast_to_i32N_decimalN(value: Option<Decimal>) -> Option<i32>
{
    match value {
        None => None,
        Some(x) => x.to_i32(),
    }
}

#[inline]
pub fn cast_to_i32N_d(value: F64) -> Option<i32>
{
    Some(value.into_inner() as i32)
}

#[inline]
pub fn cast_to_i32N_dN(value: Option<F64>) -> Option<i32>
{
    value.map(|x| x.into_inner() as i32)
}

#[inline]
pub fn cast_to_i32N_f(value: F32) -> Option<i32>
{
    Some(value.into_inner() as i32)
}

#[inline]
pub fn cast_to_i32N_fN(value: Option<F32>) -> Option<i32>
{
    value.map(|x| x.into_inner() as i32)
}

#[inline]
pub fn cast_to_i32N_s(value: String) -> Option<i32>
{
    match value.parse() {
        Err(_) => Some(0),
        Ok(x) => Some(x),
    }
}

#[inline]
pub fn cast_to_i32N_sN(value: Option<String>) -> Option<i32>
{
    match value {
        None => None,
        Some(x) => match x.parse() {
            Err(_) => Some(0),
            Ok(y) => Some(y),
        },
    }
}

#[inline]
pub fn cast_to_i32N_i(value: isize) -> Option<i32>
{
    Some(value as i32)
}

#[inline]
pub fn cast_to_i32N_i16(value: i16) -> Option<i32>
{
    Some(value as i32)
}

#[inline]
pub fn cast_to_i32N_i16N(value: Option<i16>) -> Option<i32>
{
    value.map(|x| x as i32)
}

#[inline]
pub fn cast_to_i32N_i32(value: i32) -> Option<i32>
{
    Some(value as i32)
}

#[inline]
pub fn cast_to_i32N_i32N(value: Option<i32>) -> Option<i32>
{
    value
}

#[inline]
pub fn cast_to_i32N_i64(value: i64) -> Option<i32>
{
    Some(value as i32)
}

#[inline]
pub fn cast_to_i32N_i64N(value: Option<i64>) -> Option<i32>
{
    value.map(|x| x as i32)
}

#[inline]
pub fn cast_to_i32N_u(value: usize) -> Option<i32>
{
    Some(value as i32)
}

/////////// cast to i64

#[inline]
pub fn cast_to_i64_b(value: bool) -> i64
{
    if value { 1 } else { 0 }
}

#[inline]
pub fn cast_to_i64_bN(value: Option<bool>) -> i64
{
    if value.unwrap() {1} else {0}
}

#[inline]
pub fn cast_to_i64_decimal(value: Decimal) -> i64
{
    value.to_i64().unwrap()
}

#[inline]
pub fn cast_to_i64_decimalN(value: Option<Decimal>) -> i64
{
    value.unwrap().to_i64().unwrap()
}

#[inline]
pub fn cast_to_i64_d(value: F64) -> i64
{
    value.into_inner() as i64
}

#[inline]
pub fn cast_to_i64_dN(value: Option<F64>) -> i64
{
    value.unwrap().into_inner() as i64
}

#[inline]
pub fn cast_to_i64_f(value: F32) -> i64
{
    value.into_inner() as i64
}

#[inline]
pub fn cast_to_i64_fN(value: Option<F32>) -> i64
{
    value.unwrap().into_inner() as i64
}

#[inline]
pub fn cast_to_i64_s(value: String) -> i64
{
    match value.parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i64_sN(value: Option<String>) -> i64
{
    match value.unwrap().parse() {
        Err(_) => 0,
        Ok(x) => x,
    }
}

#[inline]
pub fn cast_to_i64_i(value: isize) -> i64
{
    value as i64
}

#[inline]
pub fn cast_to_i64_i16(value: i16) -> i64
{
    value as i64
}

#[inline]
pub fn cast_to_i64_i16N(value: Option<i16>) -> i64
{
    value.unwrap() as i64
}

#[inline]
pub fn cast_to_i64_i32(value: i32) -> i64
{
    value as i64
}

#[inline]
pub fn cast_to_i64_i32N(value: Option<i32>) -> i64
{
    value.unwrap() as i64
}

#[inline]
pub fn cast_to_i64_i64(value: i64) -> i64
{
    value
}

#[inline]
pub fn cast_to_i64_i64N(value: Option<i64>) -> i64
{
    value.unwrap()
}

#[inline]
pub fn cast_to_i64_u(value: usize) -> i64
{
    value as i64
}

#[inline]
pub fn cast_to_i64_ShortInterval(value: ShortInterval) -> i64
{
    value.milliseconds()
}

#[inline]
pub fn cast_to_i64_LongInterval(value: LongInterval) -> i64
{
    value.days() as i64
}

/////////// cast to i64N

#[inline]
pub fn cast_to_i64N_b(value: bool) -> Option<i64>
{
    if value { Some(1) } else { Some(0) }
}

#[inline]
pub fn cast_to_i64N_bN(value: Option<bool>) -> Option<i64>
{
    value.map(|x| if x {1} else {0})
}

#[inline]
pub fn cast_to_i64N_decimal(value: Decimal) -> Option<i64>
{
    value.to_i64()
}

#[inline]
pub fn cast_to_i64N_decimalN(value: Option<Decimal>) -> Option<i64>
{
    match value {
        None => None,
        Some(x) => x.to_i64(),
    }
}

#[inline]
pub fn cast_to_i64N_d(value: F64) -> Option<i64>
{
    Some(value.into_inner() as i64)
}

#[inline]
pub fn cast_to_i64N_dN(value: Option<F64>) -> Option<i64>
{
    value.map(|x| x.into_inner() as i64)
}

#[inline]
pub fn cast_to_i64N_f(value: F32) -> Option<i64>
{
    Some(value.into_inner() as i64)
}

#[inline]
pub fn cast_to_i64N_fN(value: Option<F32>) -> Option<i64>
{
    value.map(|x| x.into_inner() as i64)
}

#[inline]
pub fn cast_to_i64N_s(value: String) -> Option<i64>
{
    match value.parse() {
        Err(_) => Some(0),
        Ok(x) => Some(x),
    }
}

#[inline]
pub fn cast_to_i64N_sN(value: Option<String>) -> Option<i64>
{
    match value {
        None => None,
        Some(x) => match x.parse() {
            Err(_) => Some(0),
            Ok(y) => Some(y),
        },
    }
}

#[inline]
pub fn cast_to_i64N_i(value: isize) -> Option<i64>
{
    Some(value as i64)
}

#[inline]
pub fn cast_to_i64N_i16(value: i16) -> Option<i64>
{
    Some(value as i64)
}

#[inline]
pub fn cast_to_i64N_i16N(value: Option<i16>) -> Option<i64>
{
    value.map(|x| x as i64)
}

#[inline]
pub fn cast_to_i64N_i32(value: i32) -> Option<i64>
{
    Some(value as i64)
}

#[inline]
pub fn cast_to_i64N_i32N(value: Option<i32>) -> Option<i64>
{
    value.map(|x| x as i64)
}

#[inline]
pub fn cast_to_i64N_i64(value: i64) -> Option<i64>
{
    Some(value)
}

#[inline]
pub fn cast_to_i64N_i64N(value: Option<i64>) -> Option<i64>
{
    value
}

#[inline]
pub fn cast_to_i64N_u(value: usize) -> Option<i64>
{
    Some(value as i64)
}

//////// casts to Short interval

#[inline]
pub fn cast_to_ShortInterval_i64(value: i64) -> ShortInterval
{
    ShortInterval::from(value)
}
