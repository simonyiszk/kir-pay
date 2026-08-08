import { Item } from '@/lib/api/model.ts'

export type CustomItem = {
  name: string
  price: number
}

export type CustomCartEntry = CustomItem & {
  quantity: number
}

type CartEntry = {
  item: Item
  quantity: number
}

export type Cart = {
  items: CartEntry[]
  customEntries: CustomCartEntry[]
}

export const EmptyCart: Cart = {
  customEntries: [],
  items: []
}

const findEntry = (cart: Cart, item: Item) => cart.items.find((existing) => existing.item.id === item.id)

export const addItem = (cart: Cart, item: Item): Cart => {
  const existingItem = findEntry(cart, item)
  if (existingItem !== undefined) {
    return {
      items: cart.items.map((entry) => (entry.item.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry)),
      customEntries: cart.customEntries
    }
  }
  const newItems = [...cart.items, { item, quantity: 1 }]
  newItems.sort((a, b) => a.item.name.localeCompare(b.item.name))
  return { items: newItems, customEntries: cart.customEntries }
}

export const removeItem = (cart: Cart, item: Item): Cart => {
  const existingItem = findEntry(cart, item)
  if (existingItem === undefined) {
    return cart
  }
  if (existingItem.quantity <= 1) {
    return {
      items: cart.items.filter((entry) => entry.item.id !== item.id),
      customEntries: cart.customEntries
    }
  }
  return {
    items: cart.items.map((entry) => (entry.item.id === item.id ? { ...entry, quantity: entry.quantity - 1 } : entry)),
    customEntries: cart.customEntries
  }
}

export const getItemQuantity = (cart: Cart, item: Item) => findEntry(cart, item)?.quantity ?? 0

const findCustomEntry = (cart: Cart, item: CustomItem) =>
  cart.customEntries.find((entry) => entry.name === item.name && entry.price === item.price)

export const addCustomItem = (cart: Cart, item: CustomItem): Cart => {
  const existingItem = findCustomEntry(cart, item)
  if (existingItem !== undefined) {
    return {
      customEntries: cart.customEntries.map((entry) =>
        entry.name === item.name && entry.price === item.price ? { ...entry, quantity: entry.quantity + 1 } : entry
      ),
      items: cart.items
    }
  }
  const newCustomEntries = [...cart.customEntries, { ...item, quantity: 1 }]
  newCustomEntries.sort((a, b) => a.name.localeCompare(b.name))
  return { customEntries: newCustomEntries, items: cart.items }
}

export const removeCustomItem = (cart: Cart, item: CustomItem): Cart => {
  const existingItem = findCustomEntry(cart, item)
  if (existingItem === undefined) {
    return cart
  }
  if (existingItem.quantity <= 1) {
    return {
      customEntries: cart.customEntries.filter((entry) => entry.name !== item.name || entry.price !== item.price),
      items: cart.items
    }
  }
  return {
    customEntries: cart.customEntries.map((entry) =>
      entry.name === item.name && entry.price === item.price ? { ...entry, quantity: entry.quantity - 1 } : entry
    ),
    items: cart.items
  }
}

export const getCustomItemQuantity = (cart: Cart, item: CustomItem) => findCustomEntry(cart, item)?.quantity ?? 0

export const getCartTotal = (cart: Cart): number =>
  cart.items.reduce((sum, item) => sum + item.quantity * item.item.cost, 0) +
  cart.customEntries.reduce((sum, item) => sum + item.quantity * item.price, 0)

export const getCartTotalCount = (cart: Cart): number =>
  cart.items.reduce((sum, item) => sum + item.quantity, 0) + cart.customEntries.reduce((sum, item) => sum + item.quantity, 0)
