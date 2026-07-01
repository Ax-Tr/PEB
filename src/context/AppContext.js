import React, { createContext, useContext, useReducer } from 'react';
import {
  initialCustomers,
  initialEmployees,
  initialVendors,
  initialTransactions,
  initialInstalments,
} from '../data/mockData';
import { generateId } from '../utils/helpers';

const AppContext = createContext();

const initialState = {
  customers: initialCustomers,
  employees: initialEmployees,
  vendors: initialVendors,
  transactions: initialTransactions,
  instalments: initialInstalments,
};

function appReducer(state, action) {
  switch (action.type) {
    case 'ADD_CUSTOMER':
      return {
        ...state,
        customers: [...state.customers, { ...action.payload, id: generateId() }],
      };

    case 'ADD_VENDOR':
      return {
        ...state,
        vendors: [...state.vendors, { ...action.payload, id: generateId() }],
      };

    case 'ADD_TRANSACTION':
      return {
        ...state,
        transactions: [{ ...action.payload, id: generateId() }, ...state.transactions],
      };

    case 'ADD_INSTALMENT':
      return {
        ...state,
        instalments: [...state.instalments, { ...action.payload, id: generateId() }],
      };

    case 'UPDATE_VENDOR_BALANCE':
      return {
        ...state,
        vendors: state.vendors.map((v) =>
          v.id === action.payload.id
            ? { ...v, outstandingBalance: action.payload.newBalance }
            : v
        ),
      };

    case 'UPDATE_INSTALMENT':
      return {
        ...state,
        instalments: state.instalments.map((inst) =>
          inst.id === action.payload.id ? { ...inst, ...action.payload.updates } : inst
        ),
      };

    default:
      return state;
  }
}

export function AppProvider({ children }) {
  const [state, dispatch] = useReducer(appReducer, initialState);

  const addCustomer = (customer) => dispatch({ type: 'ADD_CUSTOMER', payload: customer });
  const addVendor = (vendor) => dispatch({ type: 'ADD_VENDOR', payload: vendor });
  const addTransaction = (transaction) => dispatch({ type: 'ADD_TRANSACTION', payload: transaction });
  const addInstalment = (instalment) => dispatch({ type: 'ADD_INSTALMENT', payload: instalment });
  const updateVendorBalance = (id, newBalance) =>
    dispatch({ type: 'UPDATE_VENDOR_BALANCE', payload: { id, newBalance } });
  const updateInstalment = (id, updates) =>
    dispatch({ type: 'UPDATE_INSTALMENT', payload: { id, updates } });

  // Computed analytics
  const analytics = {
    totalRevenue: state.transactions
      .filter((t) => t.type === 'receive' && t.status === 'completed')
      .reduce((sum, t) => sum + t.amount, 0),
    totalEmployeeCost: state.transactions
      .filter((t) => t.type === 'pay_employee')
      .reduce((sum, t) => sum + t.amount, 0),
    totalVendorCost: state.transactions
      .filter((t) => t.type === 'pay_vendor')
      .reduce((sum, t) => sum + t.amount, 0),
    get totalCost() {
      return this.totalEmployeeCost + this.totalVendorCost;
    },
    get grossProfit() {
      return this.totalRevenue - this.totalCost;
    },
    vendorPayables: state.vendors.reduce((sum, v) => sum + v.outstandingBalance, 0),
    receivables: state.instalments
      .filter((i) => i.type === 'receivable')
      .reduce((sum, i) => sum + i.remainingBalance, 0),
  };

  return (
    <AppContext.Provider
      value={{
        ...state,
        analytics,
        addCustomer,
        addVendor,
        addTransaction,
        addInstalment,
        updateVendorBalance,
        updateInstalment,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useAppContext() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useAppContext must be used within an AppProvider');
  }
  return context;
}
