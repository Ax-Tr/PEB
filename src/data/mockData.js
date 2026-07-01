// Mock Data Models for PEB Application

export const initialCustomers = [
  { id: '1', name: 'Rahul Sharma', mobile: '9876543210', email: 'rahul@email.com' },
  { id: '2', name: 'Priya Patel', mobile: '9876543211', email: 'priya@email.com' },
  { id: '3', name: 'Amit Kumar', mobile: '9876543212', email: 'amit@email.com' },
  { id: '4', name: 'Sneha Reddy', mobile: '9876543213', email: 'sneha@email.com' },
  { id: '5', name: 'Vikram Singh', mobile: '9876543214', email: 'vikram@email.com' },
];

export const initialEmployees = [
  { id: '1', name: 'Ankit Verma', grossSalary: 45000, mobile: '9988776601', department: 'Engineering' },
  { id: '2', name: 'Meena Joshi', grossSalary: 38000, mobile: '9988776602', department: 'Marketing' },
  { id: '3', name: 'Rajesh Nair', grossSalary: 52000, mobile: '9988776603', department: 'Sales' },
  { id: '4', name: 'Pooja Gupta', grossSalary: 42000, mobile: '9988776604', department: 'HR' },
  { id: '5', name: 'Suresh Iyer', grossSalary: 60000, mobile: '9988776605', department: 'Engineering' },
];

export const initialVendors = [
  { id: '1', name: 'Tech Solutions Pvt Ltd', bankDetails: 'HDFC - 1234567890', upi: 'techsolutions@upi', outstandingBalance: 25000 },
  { id: '2', name: 'Office Supplies Co', bankDetails: 'SBI - 9876543210', upi: 'officesupplies@upi', outstandingBalance: 12000 },
  { id: '3', name: 'Raw Materials Inc', bankDetails: 'ICICI - 5678901234', upi: 'rawmaterials@upi', outstandingBalance: 45000 },
  { id: '4', name: 'Logistics Pro', bankDetails: 'Axis - 3456789012', upi: 'logisticspro@upi', outstandingBalance: 8000 },
];

export const initialTransactions = [
  { id: '1', type: 'receive', amount: 15000, date: '2026-04-15', status: 'completed', userId: '1', userName: 'Rahul Sharma', description: 'Web Design Services' },
  { id: '2', type: 'receive', amount: 8500, date: '2026-04-14', status: 'completed', userId: '2', userName: 'Priya Patel', description: 'Logo Design' },
  { id: '3', type: 'pay_employee', amount: 45000, date: '2026-04-01', status: 'completed', userId: '1', userName: 'Ankit Verma', description: 'March Salary' },
  { id: '4', type: 'pay_employee', amount: 38000, date: '2026-04-01', status: 'completed', userId: '2', userName: 'Meena Joshi', description: 'March Salary' },
  { id: '5', type: 'pay_vendor', amount: 20000, date: '2026-04-10', status: 'completed', userId: '1', userName: 'Tech Solutions Pvt Ltd', description: 'Server Equipment' },
  { id: '6', type: 'receive', amount: 22000, date: '2026-04-12', status: 'completed', userId: '3', userName: 'Amit Kumar', description: 'Consulting Fee' },
  { id: '7', type: 'pay_vendor', amount: 8000, date: '2026-04-08', status: 'completed', userId: '2', userName: 'Office Supplies Co', description: 'Stationery' },
  { id: '8', type: 'receive', amount: 35000, date: '2026-04-16', status: 'completed', userId: '4', userName: 'Sneha Reddy', description: 'App Development' },
  { id: '9', type: 'pay_employee', amount: 52000, date: '2026-04-01', status: 'completed', userId: '3', userName: 'Rajesh Nair', description: 'March Salary' },
  { id: '10', type: 'receive', amount: 12000, date: '2026-04-11', status: 'partial', userId: '5', userName: 'Vikram Singh', description: 'SEO Services' },
];

export const initialInstalments = [
  { id: '1', type: 'receivable', customerId: '5', customerName: 'Vikram Singh', totalAmount: 20000, amountReceived: 12000, remainingBalance: 8000, numberOfEMIs: 2, emis: [
    { emiNumber: 1, amount: 4000, dueDate: '2026-05-01', status: 'pending' },
    { emiNumber: 2, amount: 4000, dueDate: '2026-06-01', status: 'pending' },
  ]},
  { id: '2', type: 'payable', vendorId: '3', vendorName: 'Raw Materials Inc', totalAmount: 45000, amountPaid: 0, remainingBalance: 45000, nextDueDate: '2026-05-15', emis: [
    { emiNumber: 1, amount: 22500, dueDate: '2026-05-15', status: 'pending' },
    { emiNumber: 2, amount: 22500, dueDate: '2026-06-15', status: 'pending' },
  ]},
];
